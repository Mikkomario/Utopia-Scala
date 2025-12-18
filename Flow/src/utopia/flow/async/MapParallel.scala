package utopia.flow.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.FlattenParallelResults.{FlattenParallelTries, FlattenParallelTryCatches}
import utopia.flow.async.FlattenParallelResultsUsing.{FlattenParallelTriesUsing, FlattenParallelTryCatchesUsing}
import utopia.flow.async.MapParallel.{MapParallelAsync, MapParallelSync}
import utopia.flow.async.context.ActionQueue.QueuedAction
import utopia.flow.async.context.{AccessQueue, ActionQueue}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
import utopia.flow.collection.mutable.builder.{ParallelBuilder, TryCatchBuilder}
import utopia.flow.util.result.TryCatch
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object MapCollectionParallel
{
	// OTHER    ------------------------
	
	/**
	 * @param coll Collection to map
	 * @param exc Implicit execution context
	 * @tparam A Type of the items in 'coll'
	 * @return An interface for preparing / performing parallel mapping operations on 'coll'
	 */
	def apply[A](coll: IterableOnce[A])(implicit exc: ExecutionContext) = new MapCollectionParallelFactory[A](coll)
	/**
	 * @param coll Collection to map
	 * @param map A prepared mapping operation
	 * @param exc Implicit execution context
	 * @tparam A Type of the items in 'coll'
	 * @return An interface for performing parallel mapping operations on 'coll'
	 */
	def apply[A, B, To](coll: IterableOnce[A], map: MapParallel[A, B, _, To])
	                   (implicit exc: ExecutionContext): MapCollectionParallel[B, To] =
		new _MapCollectionParallel[A, B, To](coll, map)
	
	
	// NESTED   ------------------------
	
	class MapCollectionParallelFactory[+A](coll: IterableOnce[A])(implicit exc: ExecutionContext)
	{
		// COMPUTED --------------------
		
		/**
		 * @return An interface for performing asynchronous mapping operations
		 */
		def async = new MapCollectionParallelAsyncFactory[A](coll)
		
		
		// OTHER    --------------------
		
		/**
		 * Prepares parallel mapping
		 * @param f A mapping function. Expected to block.
		 * @tparam B Type of mapping output.
		 * @return A prepared parallel mapping
		 */
		def map[B](f: A => B) = apply { _.map(f) }
		/**
		 * Prepares parallel mapping
		 * @param f A mapping function. May yield a failure. Expected to block.
		 * @tparam B Type of mapping output.
		 * @return A prepared parallel mapping
		 */
		def tryMap[B](f: A => Try[B]) = apply { _.tryMap(f) }
		/**
		 * Prepares parallel mapping
		 * @param f A mapping function. May yield a full or a partial failure. Expected to block.
		 * @tparam B Type of mapping output.
		 * @return A prepared parallel mapping
		 */
		def tryMapCatching[B](f: A => TryCatch[B]) =
			apply { _.tryMapCatching(f) }
		
		private def apply[B, To](f: MapParallelSync.type => MapParallel[A, B, _, To]) =
			MapCollectionParallel(coll, f(MapParallelSync))
	}
	
	class MapCollectionParallelAsyncFactory[+A](coll: IterableOnce[A])(implicit exc: ExecutionContext)
	{
		/**
		 * Prepares parallel mapping
		 * @param f A mapping function. Yields a Future. Not expected to block.
		 * @tparam B Type of mapping output.
		 * @return A prepared parallel mapping
		 */
		def map[B](f: A => Future[B]) = apply { _.map(f) }
		/**
		 * Prepares parallel mapping
		 * @param f A mapping function. Yields a Future that may yield a failure. Not expected to block.
		 * @tparam B Type of mapping output.
		 * @return A prepared parallel mapping
		 */
		def tryMap[B](f: A => Future[Try[B]]) = apply { _.tryMap(f) }
		/**
		 * Prepares parallel mapping
		 * @param f A mapping function. Yields a Future that may yield a full or a partial failure.
		 *          Not expected to block.
		 * @tparam B Type of mapping output.
		 * @return A prepared parallel mapping
		 */
		def tryMapCatching[B](f: A => Future[TryCatch[B]]) =
			apply { _.tryMapCatching(f) }
			
		private def apply[B, To](f: MapParallelAsync.type => MapParallel[A, B, _, To]) =
			MapCollectionParallel(coll, f(MapParallelAsync))
	}
	
	private class _MapCollectionParallel[-I, O, +To](coll: IterableOnce[I], map: MapParallel[I, O, _, To])
	                                                (implicit exc: ExecutionContext)
		extends MapCollectionParallel[O, To]
	{
		override def all = map(coll)
		
		override def apply(maxWidth: Int, bufferSize: => Int)(implicit log: Logger) =
			map(coll, maxWidth, bufferSize)
		override def using(accessQueue: AccessQueue[ActionQueue], bufferSize: Int) =
			map(coll, accessQueue, bufferSize)
		
		override def to[To2 <: Iterable[_]](newBuilder: => mutable.Builder[O, To2]) =
			new _MapCollectionParallel[I, O, To2](coll, map.to(newBuilder))
	}
}

/**
 * Provides functions for performing parallel mapping on a specific (wrapped) collection
 * @tparam A Type of the mapped items
 * @tparam To Type of the resulting collection
 */
trait MapCollectionParallel[A, +To]
{
	// ABSTRACT --------------------------
	
	/**
	 * Maps all items in this collection in parallel.
	 *
	 * Note: Depending on the size of this collection, this operation may use a large number of parallel threads.
	 *       If you want to limit the number of threads used, use [[apply]] or [[using]] instead.
	 *
	 * @return A future that resolves once all items in this collection have been mapped
	 */
	def all: Future[TryCatch[To]]
	
	/**
	 * Maps all items in this collection using parallel mapping.
	 * @param maxWidth Maximum number of parallel mapping operations at any one time.
	 * @param bufferSize Maximum number of queued (but not started) mapping operations at one time.
	 *                   Default = 'width' or 8, whichever is higher.
	 * @param log Implicit logging implementation
	 * @return A future that resolves once all items in this collection have been mapped
	 */
	def apply(maxWidth: Int, bufferSize: => Int)(implicit log: Logger): Future[TryCatch[To]]
	/**
	 * Maps all items in this collection using parallel mapping.
	 * Utilizes a shared [[ActionQueue]].
	 *
	 * Note: If 'accessQueue' is not used in other operations, it is more efficient to use [[apply]] instead.
	 * @param accessQueue [[AccessQueue]], which provides ordered access to the [[ActionQueue]]
	 *                    that's utilized in this mapping operation.
	 * @param bufferSize Maximum number of queued (but not started) mapping operations at one time.
	 *                   Default = 'width' or 8, whichever is higher.
	 * @return A future that resolves once all items in this collection have been mapped
	 */
	def using(accessQueue: AccessQueue[ActionQueue], bufferSize: Int): Future[TryCatch[To]]
	
	/**
	 * Changes the built output collection-type
	 * @param newBuilder A builder used for building the resulting collection
	 * @tparam To2 Type of the built collection
	 * @return A copy of this interface using the specified builder
	 */
	def to[To2 <: Iterable[_]](newBuilder: => mutable.Builder[A, To2]): MapCollectionParallel[A, To2]
	
	
	// COMPUTED ------------------------
	
	/**
	 * Changes the built output collection-type to [[Vector]]
	 * @return A copy of this interface which builds Vectors
	 */
	def toVector = to { new VectorBuilder[A] }
	/**
	 * Changes the built output collection-type to [[Set]]
	 * @return A copy of this interface which builds Sets
	 */
	def toSet = to[Set[A]] { mutable.Set[A]().mapResult { _.toSet[A] } }
	
	
	// OTHER    ------------------------
	
	/**
	 * Maps all items in this collection using parallel mapping.
	 * @param maxWidth Maximum number of parallel mapping operations at any one time.
	 * @param log Implicit logging implementation
	 * @return A future that resolves once all items in this collection have been mapped
	 */
	def apply(maxWidth: Int)(implicit log: Logger): Future[TryCatch[To]] = apply(maxWidth, maxWidth max 8)
}

object MapParallel
{
	// COMPUTED    -------------------------
	
	/**
	 * @return Access to synchronous (i.e. blocking) mapping operations
	 */
	def sync = MapParallelSync
	/**
	 * @return Access to asynchronous mapping operations, which yield [[Future]]s
	 */
	def async = MapParallelAsync
	
	
	// NESTED   ----------------------------
	
	object MapParallelSync
	{
		// OTHER    ------------------------
		
		/**
		 * Prepares parallel mapping logic
		 * @param f A mapping function. Expected to block.
		 * @tparam A Type of mapping input.
		 * @tparam B Type of mapping output.
		 * @return A new mapping implementation
		 */
		def map[A, B](f: A => B): MapParallel[A, B, B, IndexedSeq[B]] =
			new _MapParallelSync[A, B, B, IndexedSeq[B]](f, FlattenParallelResults.toSeq[B])
		/**
		 * Prepares parallel mapping logic
		 * @param f A mapping function. May yield a failure. Expected to block.
		 * @tparam A Type of mapping input.
		 * @tparam B Type of mapping output.
		 * @return A new mapping implementation
		 */
		def tryMap[A, B](f: A => Try[B]): MapParallel[A, B, Try[B], IndexedSeq[B]] =
			new _MapParallelSync[A, B, Try[B], IndexedSeq[B]](f, FlattenParallelTries.toSeq[B])
		/**
		 * Prepares parallel mapping logic
		 * @param f A mapping function. May yield a full or a partial failure. Expected to block.
		 * @tparam A Type of mapping input.
		 * @tparam B Type of mapping output.
		 * @return A new mapping implementation
		 */
		def tryMapCatching[A, B](f: A => TryCatch[B]): MapParallel[A, B, TryCatch[B], IndexedSeq[B]] =
			new _MapParallelSync[A, B, TryCatch[B], IndexedSeq[B]](f, FlattenParallelTryCatches.toSeq[B])
		
		
		// NESTED   ------------------------
		
		private class _MapParallelSync[-I, +O, R, +To](f: I => R,
		                                               override val flatten: FlattenParallelResults[O, R, To])
			extends MapParallelSync[I, O, R, To]
		{
			override protected def map(item: I): R = f(item)
			
			override def to[To2 <: Iterable[_]](newBuilder: => mutable.Builder[O, To2]): MapParallel[I, O, R, To2] =
				new _MapParallelSync[I, O, R, To2](f, flatten.using(newBuilder))
		}
	}
	private trait MapParallelSync[-I, +O, R, +To] extends MapParallel[I, O, R, To]
	{
		// ABSTRACT ----------------------
		
		/**
		 * @param item Item to map
		 * @return The mapping result
		 */
		protected def map(item: I): R
		
		
		// IMPLEMENTED  ------------------
		
		override def push(queue: ActionQueue, item: I): QueuedAction[R] = queue.push { map(item) }
		override def pushAll(queue: ActionQueue, items: IterableOnce[I]): Seq[QueuedAction[R]] =
			queue.pushAll(items.iterator.map { a => View { map(a) } })
		
		override def apply(item: I)(implicit exc: ExecutionContext): Future[R] = Future { map(item) }
		override def apply(coll: IterableOnce[I])(implicit exc: ExecutionContext): Future[TryCatch[To]] =
			flatten(coll.iterator.map { a => Future { map(a) } }.toOptimizedSeq)
		
		override protected def consecutively(coll: IterableOnce[I])
		                                    (implicit exc: ExecutionContext): Future[TryCatch[To]] =
			Future { flatten.newBuilder.addAll(coll.iterator.map(map)).result() }
	}
	
	object MapParallelAsync
	{
		// OTHER    ------------------------
		
		/**
		 * Prepares parallel mapping logic
		 * @param f A mapping function. Not expected to block. Yields a Future.
		 * @tparam A Type of mapping input.
		 * @tparam B Type of mapping output.
		 * @return A new mapping implementation
		 */
		def map[A, B](f: A => Future[B]): MapParallel[A, B, B, IndexedSeq[B]] =
			new _MapParallelAsync(f, FlattenParallelResults.toSeq)
		/**
		 * Prepares parallel mapping logic
		 * @param f A mapping function. Not expected to block. Yields a Future that may contain a failure.
		 * @tparam A Type of mapping input.
		 * @tparam B Type of mapping output.
		 * @return A new mapping implementation
		 */
		def tryMap[A, B](f: A => Future[Try[B]]): MapParallel[A, B, Try[B], IndexedSeq[B]] =
			new _MapParallelAsync(f, FlattenParallelTries.toSeq)
		/**
		 * Prepares parallel mapping logic
		 * @param f A mapping function. Not expected to block.
		 *          Yields a Future that may contain a full or a partial failure.
		 * @tparam A Type of mapping input.
		 * @tparam B Type of mapping output.
		 * @return A new mapping implementation
		 */
		def tryMapCatching[A, B](f: A => Future[TryCatch[B]]): MapParallel[A, B, TryCatch[B], IndexedSeq[B]] =
			new _MapParallelAsync(f, FlattenParallelTryCatches.toSeq)
		
		
		// NESTED   ------------------------
		
		// WET WET
		private class _MapParallelAsync[-I, +O, R, +To <: Iterable[_]](f: I => Future[R],
		                                                               override val flatten: FlattenParallelResults[O, R, To])
			extends MapParallelAsync[I, O, R, To]
		{
			override protected def map(item: I): Future[R] = f(item)
			
			override def to[To2 <: Iterable[_]](newBuilder: => mutable.Builder[O, To2]): MapParallel[I, O, R, To2] =
				new _MapParallelAsync[I, O, R, To2](f, flatten.using(newBuilder))
		}
	}
	private trait MapParallelAsync[-I, +O, R, +To] extends MapParallel[I, O, R, To]
	{
		// ABSTRACT ------------------------
		
		/**
		 * Performs an asynchronous mapping operation
		 * @param item An item to map
		 * @return A future that resolves into the mapping result
		 */
		protected def map(item: I): Future[R]
		
		
		// IMPLEMENTED  --------------------
		
		override def push(queue: ActionQueue, item: I): QueuedAction[R] = queue.pushAsync { map(item) }
		override def pushAll(queue: ActionQueue, items: IterableOnce[I]): Seq[QueuedAction[R]] =
			queue.pushAllAsync(items.iterator.map { a => View { map(a) } })
		
		override def apply(item: I)(implicit exc: ExecutionContext): Future[R] = map(item)
		override def apply(coll: IterableOnce[I])(implicit exc: ExecutionContext): Future[TryCatch[To]] =
			flatten(coll.iterator.map(map).toOptimizedSeq)
		
		override protected def consecutively(coll: IterableOnce[I])
		                                    (implicit exc: ExecutionContext): Future[TryCatch[To]] =
			flatten(coll.iterator.map(map))
	}
}

/**
 * An interface for preparing and performing parallel mapping functions
 * @tparam I Type of accepted mapping input (individual values)
 * @tparam O Type of mapping output, when successful (i.e. the mapping values that are collected)
 * @tparam R Type of the mapping results (e.g. Try[O])
 * @tparam To Type of the collections built (from successful values, e.g. Seq[O])
 * @author Mikko Hilpinen
 * @since 11.12.2025, v2.8
 */
trait MapParallel[-I, +O, R, +To]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Flattening implementation for building the resulting collections
	 */
	def flatten: FlattenParallelResults[O, R, To]
	
	/**
	 * @param queue Queue in which the mapping action will be performed
	 * @param item Item to be mapped
	 * @return A queued action which will yield the mapping result
	 */
	def push(queue: ActionQueue, item: I): QueuedAction[R]
	/**
	 * @param queue Queue in which the mapping action will be performed
	 * @param items Items to be mapped
	 * @return A queued actions which will yield the mapping results
	 */
	def pushAll(queue: ActionQueue, items: IterableOnce[I]): Seq[QueuedAction[R]]
	
	/**
	 * Maps an individual item, asynchronously
	 * @param item An item to map
	 * @param exc Implicit execution context
	 * @return A future that resolves into the mapping result, once mapping has completed
	 */
	def apply(item: I)(implicit exc: ExecutionContext): Future[R]
	/**
	 * Maps n items in parallel.
	 *
	 * Note: Depending on the size of 'coll', this operation may use a large number of threads at once.
	 *       If you want to limit the thread-usage, consider using other variations of this function instead.
	 *
	 * @param coll Collection to map
	 * @param exc Implicit execution context
	 * @return Future that resolves once all items in 'coll' have been mapped
	 */
	def apply(coll: IterableOnce[I])(implicit exc: ExecutionContext): Future[TryCatch[To]]
	
	/**
	 * @param newBuilder A function for constructing new collection-builders
	 * @tparam To2 Type of the built collections
	 * @return A copy of this mapping logic, which builds the resulting collections using the specified builder.
	 */
	def to[To2 <: Iterable[_]](newBuilder: => mutable.Builder[O, To2]): MapParallel[I, O, R, To2]
	
	/**
	 * Maps n items back-to-back (i.e. not utilizing parallel mapping)
	 * @param coll Collection to map
	 * @param exc Implicit execution context
	 * @return A future that resolves once all items in 'coll' have been mapped
	 */
	protected def consecutively(coll: IterableOnce[I])(implicit exc: ExecutionContext): Future[TryCatch[To]]
	
	
	// OTHER    -------------------------
	
	/**
	 * Prepares a [[ParallelBuilder]], which uses this mapping logic
	 * @param width Maximum number of parallel mapping operations at any one time
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation
	 * @return A new parallel builder
	 */
	def toBuilder(width: Int)(implicit exc: ExecutionContext, log: Logger): ParallelBuilder[I, R, To] =
		toBuilder(width, width max 8)
	/**
	 * Prepares a [[ParallelBuilder]], which uses this mapping logic
	 * @param width Maximum number of parallel mapping operations at any one time
	 * @param bufferSize Maximum number of queued (but not started) mapping operations at one time.
	 *                   Default = 'width' or 8, whichever is higher.
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation
	 * @return A new parallel builder
	 * @see [[toBuilderUsing]] for use-cases where multiple builders run in parallel
	 */
	def toBuilder(width: Int, bufferSize: Int)(implicit exc: ExecutionContext, log: Logger) =
		_toBuilderUsing(new AccessQueue[ActionQueue](ActionQueue(width)), bufferSize)
	
	/**
	 * Prepares a [[ParallelBuilder]], which uses this mapping logic.
	 * The resulting builder will use a shared [[ActionQueue]].
	 * @param accessQueue [[AccessQueue]], which provides ordered access to the [[ActionQueue]]
	 *                    that's utilized in this mapping operation.
	 * @param bufferSize Maximum number of queued (but not started) mapping operations at one time.
	 *                   Note: Specific to this builder; Not shared.
	 * @param exc Implicit execution context
	 * @return A new parallel builder
	 * @see [[toBuilder]], if you only need one builder at one time
	 */
	def toBuilderUsing(accessQueue: AccessQueue[ActionQueue], bufferSize: Int)(implicit exc: ExecutionContext) =
		_toBuilderUsing(accessQueue, bufferSize)
	
	/**
	 * Maps a collection using multiple parallel mapping-threads
	 * @param coll Collection to map
	 * @param maxWidth Maximum number of parallel mapping operations at any one time
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation
	 * @return A future that resolves once all items in 'coll' have been mapped
	 */
	def apply(coll: IterableOnce[I], maxWidth: Int)(implicit exc: ExecutionContext, log: Logger): Future[TryCatch[To]] =
		apply(coll, maxWidth, maxWidth max 8)
	/**
	 * Maps a collection using multiple parallel mapping-threads.
	 * @param coll Collection to map
	 * @param maxWidth Maximum number of parallel mapping operations at any one time
	 * @param bufferSize Maximum number of queued (but not started) mapping operations at one time.
	 *                   Default = 'width' or 8, whichever is higher.
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation
	 * @return A future that resolves once all items in 'coll' have been mapped
	 */
	def apply(coll: IterableOnce[I], maxWidth: Int, bufferSize: => Int)
	         (implicit exc: ExecutionContext, log: Logger): Future[TryCatch[To]] =
	{
		// Case: Only a single thread allowed => Maps the items in a single Future
		if (maxWidth <= 1)
			consecutively(coll)
		else {
			// Also, if the original collection is small enough, simplifies the process by not limiting thread use
			val knownSize = coll.knownSize
			// Case: This collection is empty => No mapping is required
			if (knownSize == 0)
				flatten(Empty)
			// Case: This collection contains exactly one item => Applies a single item mapping function
			else if (knownSize == 1)
				flatten(Single(apply(coll.iterator.next())))
			// Case: Maximum width fits this whole collection
			//       => Maps all items in parallel without limiting thread-usage
			else if (knownSize > 0 && knownSize <= maxWidth)
				apply(coll)
			// Case: Thread-usage limiting may be necessary => Maps the items using a ParallelBuilder
			else
				toBuilder(maxWidth, bufferSize).async.addAll(coll).result()
		}
	}
	
	/**
	 * Maps a collection using multiple parallel mapping-threads. Uses a shared [[ActionQueue]].
	 *
	 * Note: If you're not running multiple parallel mapping operations,
	 *       it's more efficient to use other versions of this function instead.
	 *
	 * @param coll Collection to map
	 * @param accessQueue [[AccessQueue]], which provides ordered access to the [[ActionQueue]]
	 *                    that's utilized in this mapping operation.
	 * @param bufferSize Maximum number of queued (but not started) mapping operations at one time.
	 *                   Default = 'width' or 8, whichever is higher.
	 * @param exc Implicit execution context
	 * @return A future that resolves once all items in 'coll' have been mapped
	 */
	def apply(coll: IterableOnce[I], accessQueue: AccessQueue[ActionQueue], bufferSize: Int)
	         (implicit exc: ExecutionContext) =
		toBuilderUsing(accessQueue, bufferSize).async.addAll(coll).result()
	
	private def _toBuilderUsing(accessQueue: AccessQueue[ActionQueue], bufferSize: Int)(implicit exc: ExecutionContext) =
		new ParallelBuilder[I, R, To](accessQueue, this, bufferSize)
}

/**
 * Common trait for the different flattening implementation -factories
 * @tparam F Type of flattening implementations built.
 *           These accept two generic parameters:
 *              1. Type of successful / collected values (A)
 *              1. Type of collections built (To)
 */
trait FlattenWithBuilderFactory[+F[_, _ <: Iterable[_]]]
{
	// ABSTRACT ---------------------------
	
	/**
	 * Prepares a new flattening implementation
	 * @param newBuilder A function that generates the builders used for generating the resulting collections
	 * @tparam A Type of the collected items (i.e. successful mapping results)
	 * @tparam To Type of the collections built
	 * @return A new flattening implementation
	 */
	def to[A, To <: Iterable[_]](newBuilder: => mutable.Builder[A, To]): F[A, To]
	
	
	// COMPUTED --------------------------
	
	/**
	 * Prepares a new flattening implementation that builds [[Vector]]s
	 * @tparam A Type of the collected items (i.e. successful mapping results)
	 * @return A new flattening implementation
	 */
	def toSeq[A] = to[A, IndexedSeq[A]](OptimizedIndexedSeq.newBuilder[A])
}

object FlattenParallelResults extends FlattenWithBuilderFactory[FlattenParallelResultsUsing]
{
	// COMPUTED --------------------------
	
	/**
	 * @return An interface used for building mapping logic for mapping results which may contain a failure
	 */
	def tries = FlattenParallelTries
	/**
	 * @return An interface used for building mapping logic for mapping results
	 *         which may contain full or partial failures
	 */
	def tryCatches = FlattenParallelTryCatches
	
	
	// IMPLEMENTED  ----------------------
	
	override def to[A, To <: Iterable[_]](newBuilder: => mutable.Builder[A, To]): FlattenParallelResultsUsing[A, To] =
		new FlattenParallelResultsUsing[A, To](newBuilder)
	
	
	// NESTED   --------------------------
	
	object FlattenParallelTries extends FlattenWithBuilderFactory[FlattenParallelTriesUsing]
	{
		override def to[A, To <: Iterable[_]](newBuilder: => mutable.Builder[A, To]): FlattenParallelTriesUsing[A, To] =
			new FlattenParallelTriesUsing(newBuilder)
	}
	
	object FlattenParallelTryCatches extends FlattenWithBuilderFactory[FlattenParallelTryCatchesUsing]
	{
		override def to[A, To <: Iterable[_]](newBuilder: => mutable.Builder[A, To]): FlattenParallelTryCatchesUsing[A, To] =
			new FlattenParallelTryCatchesUsing(newBuilder)
	}
}

/**
 * Used for building results of parallel mapping into a future collection
 * @tparam A Type of the *successful* mapping results / collected items
 * @tparam R Type of the mapping results
 * @tparam To Type of the collections built
 */
trait FlattenParallelResults[+A, -R, +To]
{
	/**
	 * @return A builder for combining mapping results
	 */
	def newBuilder: mutable.Builder[R, TryCatch[To]]
	
	/**
	 * Combines multiple parallel results into a single future / collection
	 * @param futures Mapping result -futures
	 * @param exc Implicit execution context
	 * @return A future that resolves once all 'futures' have resolved, yielding the built collection (or a failure)
	 */
	def apply(futures: IterableOnce[Future[R]])(implicit exc: ExecutionContext): Future[TryCatch[To]]
	
	/**
	 * @param newBuilder A builder to use for combining successful results
	 * @tparam To2 Type of the collections built using that builder
	 * @return A copy of this logic which uses the specified builder
	 */
	def using[To2 <: Iterable[_]](newBuilder: => mutable.Builder[A, To2]): FlattenParallelResults[A, R, To2]
}

object FlattenParallelResultsUsing
{
	// NESTED   --------------------------
	
	/**
	 * Used for building collections from parallel mapping results.
	 * Used in mapping operations that may fail (by yielding a Failure).
	 * @param _newBuilder A function for generating new collection-builders
	 * @tparam A Type of the *successful* mapping results / collected items
	 * @tparam To Type of the collections built
	 */
	class FlattenParallelTriesUsing[A, +To <: Iterable[_]](_newBuilder: => mutable.Builder[A, To])
		extends FlattenParallelResults[A, Try[A], To]
	{
		override def newBuilder = TryCatchBuilder.wrap(_newBuilder)
		
		override def using[To2 <: Iterable[_]](newBuilder: => mutable.Builder[A, To2]) =
			new FlattenParallelTriesUsing[A, To2](newBuilder)
		
		override def apply(futures: IterableOnce[Future[Try[A]]])
		                  (implicit exc: ExecutionContext): Future[TryCatch[To]] =
			futures.futureUsing(_newBuilder)
	}
	/**
	 * Used for building collections from parallel mapping results.
	 * Used in mapping operations that may fail fully or partially.
	 * @param _newBuilder A function for generating new collection-builders
	 * @tparam A Type of the *successful* mapping results / collected items
	 * @tparam To Type of the collections built
	 */
	class FlattenParallelTryCatchesUsing[A, +To <: Iterable[_]](_newBuilder: => mutable.Builder[A, To])
		extends FlattenParallelResults[A, TryCatch[A], To]
	{
		override def newBuilder =
			TryCatchBuilder.wrap(_newBuilder).catching
		
		override def using[To2 <: Iterable[_]](newBuilder: => mutable.Builder[A, To2]) =
			new FlattenParallelTryCatchesUsing[A, To2](newBuilder)
		
		override def apply(futures: IterableOnce[Future[TryCatch[A]]])
		                  (implicit exc: ExecutionContext): Future[TryCatch[To]] =
			futures.futureUsing(_newBuilder)
	}
}

/**
 * Used for building collections from parallel mapping results.
 * Used in mapping operations always succeed (unless they throw).
 * @param _newBuilder A function for generating new collection-builders
 * @tparam A Type of the *successful* mapping results / collected items
 * @tparam To Type of the collections built
 */
class FlattenParallelResultsUsing[A, +To <: Iterable[_]](_newBuilder: => mutable.Builder[A, To])
	extends FlattenParallelResults[A, A, To]
{
	override def newBuilder = TryCatchBuilder.wrap(_newBuilder).fromSuccesses
	
	override def using[To2 <: Iterable[_]](newBuilder: => mutable.Builder[A, To2]) =
		new FlattenParallelResultsUsing[A, To2](newBuilder)
	
	override def apply(futures: IterableOnce[Future[A]])(implicit exc: ExecutionContext): Future[TryCatch[To]] =
		futures.futureUsing(_newBuilder)
}
package utopia.flow.collection.mutable.builder

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ActionQueue
import utopia.flow.async.context.ActionQueue.QueuedAction
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.collection.mutable.builder.ParallelBuilder.collectResultsUsing
import utopia.flow.util.TryCatch
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

import scala.collection.immutable.VectorBuilder
import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object ParallelBuilder
{
	// OTHER    ----------------------------
	
	/**
	 * Creates a new builder which maps items in parallel. Applies the default buffer size.
	 * @param width Maximum number of parallel processes at any time
	 * @param exc   Implicit execution context
	 * @param log   Implicit logging implementation
	 * @return A new parallel builder
	 */
	def apply(width: Int)(implicit exc: ExecutionContext, log: Logger): ParallelBuilderFactory =
		apply(width, width max 8)
	/**
	 * Creates a new builder which maps items in parallel
	 * @param width Maximum number of parallel processes at any time
	 * @param bufferSize Maximum number of buffered items.
	 *                   If this buffer becomes full, this builder will start blocking the calling thread.
	 *                   Default = 'width' or 8, whichever is larger.
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation
	 * @return A new parallel builder
	 */
	def apply(width: Int, bufferSize: Int)(implicit exc: ExecutionContext, log: Logger) =
		new ParallelBuilderFactory(width, bufferSize)
	
	/**
	 * Collects asynchronously acquired results into a single collection
	 * @param futures Futures from which the results are collected
	 * @param exc Implicit execution context
	 * @tparam A Type of successful results
	 * @return A future that resolves into the collected items.
	 *         Will yield a failure if all futures failed.
	 *         Will yield a partial failure if only some of the futures failed.
	 */
	def collectResults[A](futures: IterableOnce[Future[A]])(implicit exc: ExecutionContext): Future[TryCatch[IndexedSeq[A]]] =
		collectResultsUsing(futures, OptimizedIndexedSeq.newBuilder[A])
	/**
	 * Collects asynchronously acquired results into a single collection
	 * @param futures Futures from which the results are collected
	 * @param builder A builder which collects successful results
	 * @param exc Implicit execution context
	 * @tparam A Type of successful results
	 * @tparam To Type of the resulting collection
	 * @return A future that resolves into the collected items.
	 *         Will yield a failure if all futures failed.
	 *         Will yield a partial failure if only some of the futures failed.
	 */
	def collectResultsUsing[A, To <: Iterable[_]](futures: IterableOnce[Future[A]], builder: mutable.Builder[A, To])
	                                             (implicit exc: ExecutionContext) =
		collectResults(futures.iterator, TryCatchBuilder.wrap(builder))
	
	private def collectResults[A, To](resultsIter: Iterator[Future[A]], builder: TryCatchBuilder[A, To])
	                                 (implicit exc: ExecutionContext): Future[TryCatch[To]] =
	{
		// Collects items until a pending item is encountered
		resultsIter.find { future =>
			// Case: This process was already completed => Adds the result to the appropriate buffer and moves on
			if (future.isCompleted) {
				builder += future.waitFor()
				false
			}
			// Case: This process is still pending => Moves to the next phase
			else
				true
		} match {
			// Case: A pending process found => Prepares a result future based on its result, using recursion
			case Some(pendingFuture) =>
				pendingFuture.toTryFuture.flatMap { result =>
					// Adds the resolved result to the appropriate buffer
					builder += result
					// Continues using recursion
					collectResults(resultsIter, builder)
				}
			// Case: All items were processed => Builder the resulting collection
			case None => Future.successful(builder.result())
		}
	}
	
	
	// NESTED   ----------------------------
	
	class ParallelBuilderFactory(width: Int, bufferSize: Int)(implicit exc: ExecutionContext, log: Logger)
	{
		// INITIAL CODE --------------------
		
		if (bufferSize <= 0)
			throw new IllegalArgumentException("Buffer size must be positive")
		
		
		// OTHER    ------------------------
		
		/**
		 * @param bufferSize Number of items that may be buffered into the queue at once
		 *                   (NB: The processes are not necessarily started immediately, just queued).
		 * @return A copy of this factory using the specified buffer size
		 */
		def withBufferSize(bufferSize: Int) = new ParallelBuilderFactory(width, bufferSize)
		
		/**
		 * Prepares a new builder which maps items in parallel
		 * @param f A synchronous mapping function
		 * @tparam A Type of the accepted items
		 * @tparam B Type of mapping output
		 * @return A new builder
		 */
		def map[A, B](f: A => B) =
			_apply[A, B] { (q, i) => q.push { f(i) } } { (q, items) => q.pushAll(items.map { i => View { f(i) } }) }
		/**
		 * Prepares a new builder which maps items in parallel
		 * @param f A synchronous mapping function. May yield a failure.
		 * @tparam A Type of the accepted items
		 * @tparam B Type of mapping output
		 * @return A new builder
		 */
		def tryMap[A, B](f: A => Try[B]) =
			_apply[A, B] { (q, i) => q.pushTry { f(i) } } {
				(q, items) => q.pushAllTries(items.map { i => View { f(i) } }) }
		/**
		 * Prepares a new builder which maps items in parallel
		 * @param f A mapping function which yields asynchronously resolving [[Future]]s.
		 *          Not expected to block.
		 * @tparam A Type of the accepted items
		 * @tparam B Type of mapping output
		 * @return A new builder
		 */
		def mapAsync[A, B](f: A => Future[B]) =
			_apply[A, B] { (q, i) => q.pushAsync { f(i) } } {
				(q, items) => q.pushAllAsync(items.map { i => View { f(i) } }) }
		
		private def _apply[I, A](push: (ActionQueue, I) => QueuedAction[A])
		                        (pushAll: (ActionQueue, Seq[I]) => Seq[QueuedAction[A]]) =
			new ParallelBuilder[I, A, IndexedSeq[A]](width, bufferSize)(push)(pushAll)(OptimizedIndexedSeq.newBuilder)
	}
}

/**
 * Used for building collections using multiple parallel threads
 * @tparam I Type of accepted items
 * @tparam A Type of mapped / processed items
 * @tparam To Type of built collections
 * @author Mikko Hilpinen
 * @since 07.12.2025, v2.8
 */
class ParallelBuilder[-I, A, +To <: Iterable[_]](width: Int, bufferSize: Int)
                                                (push: (ActionQueue, I) => QueuedAction[A])
                                                (pushAll: (ActionQueue, Seq[I]) => Seq[QueuedAction[A]])
                                                (newResultBuilder: => mutable.Builder[A, To])
                                                (implicit exc: ExecutionContext, log: Logger)
	extends mutable.Builder[I, Future[TryCatch[To]]]
{
	// ATTRIBUTES   --------------------------
	
	private val lazyQueue = Lazy.resettable { ActionQueue(width) }
	private val lazyBuilder = Lazy.resettable { new VectorBuilder[Future[A]] }
	
	
	// IMPLEMENTED  --------------------------
	
	override def knownSize = lazyBuilder.value.knownSize
	
	override def clear() = {
		lazyQueue.reset()
		lazyBuilder.reset()
	}
	override def result() = {
		val builder = lazyBuilder.value
		// Waits for the action queue to clear, in order to make sure that all results get added to the builder
		lazyQueue.value.emptyFuture.flatMap { _ =>
			// Once the queue is clear, processes the collected futures
			collectResultsUsing(builder.result(), newResultBuilder)
		}
	}
	
	override def addOne(elem: I) = {
		val queue = lazyQueue.value
		val builder = lazyBuilder.value
		if (queue.queueSize < bufferSize)
			builder += push(queue, elem).future
		else {
			waitUntilBufferIsEmpty(queue)
			builder += push(queue, elem).future
		}
		this
	}
	override def addAll(elems: IterableOnce[I]) = {
		// Prepares the builder and buffers the proposed items
		val queue = lazyQueue.value
		val builder = lazyBuilder.value
		val bufferedInput = OptimizedIndexedSeq.from(elems)
		
		if (bufferedInput.nonEmpty) {
			// Checks how many items may be appended without blocking
			val immediateCapacity = bufferSize - queue.queueSize
			// Case: No blocking required => Adds the items to the queue
			if (immediateCapacity >= bufferedInput.size)
				appendAll(queue, builder, bufferedInput)
			// Case: Blocking will be necessary => Appends the items in chunks
			else {
				var remainder = {
					// Case: May append some of the items now => Takes only the remainder
					if (immediateCapacity > 0) {
						val (pushNow, pushLater) = bufferedInput.splitAt(immediateCapacity)
						appendAll(queue, builder, pushNow)
						pushLater
					}
					// Case: No items may be appended => Queues them all
					else
						bufferedInput
				}
				// Waits and fills the buffer as long as more items remain
				while (remainder.nonEmpty) {
					waitUntilBufferIsEmpty(queue)
					val (pushNow, pushLater) = remainder.splitAt(bufferSize)
					appendAll(queue, builder, pushNow)
					remainder = pushLater
				}
			}
		}
		this
	}
	
	
	// OTHER    --------------------------
	
	/**
	 * @param newBuilder A function for constructing new result-builders
	 * @tparam To2 Type of the collections built
	 * @return A new parallel builder which builds collections using the specified builder-constructor
	 */
	def withResultBuilder[To2 <: Iterable[_]](newBuilder: => mutable.Builder[A, To2]) =
		new ParallelBuilder[I, A, To2](width, bufferSize)(push)(pushAll)(newBuilder)
	
	private def appendAll(queue: ActionQueue, builder: mutable.Growable[Future[A]], items: Seq[I]) =
		builder ++= pushAll(queue, items).view.map { _.future }
	
	private def waitUntilBufferIsEmpty(queue: ActionQueue) =
		queue.notPendingFlag.future.waitFor().logWithMessage("Failure while waiting for the action queue to clear")
}

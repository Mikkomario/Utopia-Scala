package utopia.flow.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ActionQueue
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.collection.BuildFrom
import scala.collection.generic.{IsIterable, IsIterableOnce}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}
import scala.language.implicitConversions

/**
  * An extension providing asynchronous collection functions
  * @author Mikko Hilpinen
  * @since 04.05.2024, v2.4
  */
@deprecated("Deprecated for removal. These were moved to CollectionExtensions, since both could not be imported at the same time", "v2.5.1")
object AsyncCollectionExtensions
{
	class AsyncIterableOnceOps[Repr, I <: IsIterableOnce[Repr]](coll: Repr, iter: I)
	{
		// ATTRIBUTES   --------------------------
		
		private lazy val ops = iter(coll)
		
		
		// OTHER    ------------------------------
		
		/**
		  * Calls a function for each item in this collection.
		  * Utilizes multiple threads in order to process up to 'maxWidth' items in parallel.
		  * @param maxWidth Maximum number of items to process in parallel at any time
		  * @param f A function that accepts an item in this collection.
		  *          Called asynchronously. Not expected to throw. Thrown errors are delegated to the 'exc'.
		  * @param exc Implicit execution context to use.
		  * @tparam U Arbitrary function result type
		  */
		def foreachParallel[U](maxWidth: Int)(f: iter.A => U)(implicit exc: ExecutionContext) = {
			val iter = ops.iterator
			if (iter.hasNext) {
				// Case: No multi-threading allowed => Uses normal foreach instead
				if (maxWidth <= 1)
					iter.foreach(f)
				else {
					val knownSize = ops.knownSize
					// Case: Known to be smaller than the thread limit => Processes all items in parallel
					if (knownSize >= 0 && knownSize <= maxWidth)
						iter.foreach { a => Future { f(a) } }
					// Case: May be larger than the thread limit
					//       => Utilizes an action queue to limit the number of parallel processes
					else {
						implicit val log: Logger = SysErrLogger
						val queue = new ActionQueue(maxWidth)
						iter.foreach { a => queue.push { f(a) }.waitUntilStarted() }
					}
				}
			}
		}
	}
	
	implicit def iterableOnceOperations[Repr](coll: Repr)
	                                         (implicit iter: IsIterableOnce[Repr]): AsyncIterableOnceOps[Repr, iter.type] =
		new AsyncIterableOnceOps(coll, iter)
	
	class AsyncIterableOps[Repr, I <: IsIterable[Repr]](coll: Repr, iter: I)
	{
		// ATTRIBUTES   --------------------------
		
		private lazy val ops = iter(coll)
		
		
		// OTHER    ------------------------------
		
		/**
		  * Maps the items within this collection using multiple threads at once
		  * @param maxWidth Maximum number of threads that may be used simultaneously
		  *                 when processing the contents of this collection
		  * @param f A mapping function. Will be called asynchronously. Expected to block, but not throw.
		  * @param bf Implicit build-from for the resulting collection
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @tparam To Type of the resulting collection
		  * @return (Successful) Mapping results
		  */
		def mapParallel[B, To](maxWidth: Int)(f: iter.A => B)
		                      (implicit bf: BuildFrom[Repr, B, To], exc: ExecutionContext): To =
		{
			// Covers the single-threaded use-case
			if (maxWidth <= 1)
				bf.fromSpecific(coll)(ops.map(f))
			else {
				// Also, if the original collection is small enough, simplifies the process by not limiting thread use
				val knownSize = ops.knownSize
				if (knownSize == 0)
					bf.fromSpecific(coll)(Iterator.empty)
				else if (knownSize == 1)
					bf.fromSpecific(coll)(Iterator.single(f(ops.head)))
				else if (knownSize >= 0 && knownSize <= maxWidth)
					mapAllParallel(f)
				else {
					// Prepares the queue for parallel processing
					implicit val log: Logger = SysErrLogger
					mapParallelUsing(new ActionQueue(maxWidth))(f)
				}
			}
		}
		/**
		  * Maps this collection in parallel using multiple threads
		  * @param queue Action queue which limits the number of parallel processes
		  * @param f A mapping function
		  * @param bf Implicit build-from for the resulting collection
		  * @param log Implicit logging implementation used for handling failures thrown by 'f'
		  * @tparam B Type of mapping results
		  * @tparam To Type of the resulting collection
		  * @return A mapped copy of this collection
		  */
		def mapParallelUsing[B, To](queue: ActionQueue)(f: iter.A => B)
		                           (implicit bf: BuildFrom[Repr, B, To], log: Logger): To =
		{
			if (queue.maxWidth <= 1)
				bf.fromSpecific(coll)(ops.map(f))
			else {
				val knownSize = ops.knownSize
				if (knownSize == 0)
					bf.fromSpecific(coll)(Iterator.empty)
				else if (knownSize == 1)
					bf.fromSpecific(coll)(Iterator.single(f(ops.head)))
				else {
					val builder = bf.newBuilder(coll)
					ops
						// Maps by starting as many actions as is possible in parallel
						// Waits if some action can't be started due to capacity issues
						.map { a =>
							val action = queue.push { f(a) }
							action.waitUntilStarted()
							action
						}
						// Collects the results from each mapping action
						.foreach { action =>
							action.waitFor() match {
								// Case: Mapping succeeded => Adds the item to success results
								case Success(res) => builder += res
								// Case: Mapping failed => Records a failure
								case Failure(error) => log(error)
							}
						}
					builder.result()
				}
			}
		}
		
		/**
		  * Maps all items in this collection in parallel with each other, utilizing multiple threads.
		  * This function is more appropriate for small-size collection. For larger collections,
		  * consider using [[mapParallelUsing]] instead.
		  * @param f A mapping function. Called asynchronously. Not expected to throw.
		  * @param bf Implicit build-from for the resulting collection
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @tparam To Type of the resulting collection
		  * @return (Successful) mapping results
		  */
		def mapAllParallel[B, To](f: iter.A => B)(implicit bf: BuildFrom[Repr, B, To], exc: ExecutionContext) = {
			val builder = bf.newBuilder(coll)
			ops.map { a => Future { f(a) } }.foreach { future =>
				future.waitFor() match {
					case Success(res) => builder += res
					case Failure(error) => exc.reportFailure(error)
				}
			}
			builder.result()
		}
	}
	
	implicit def iterableOperations[Repr](coll: Repr)
	                                     (implicit iter: IsIterable[Repr]): AsyncIterableOps[Repr, iter.type] =
		new AsyncIterableOps(coll, iter)
}

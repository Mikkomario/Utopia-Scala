package utopia.flow.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ActionQueue

import scala.collection.BuildFrom
import scala.collection.generic.IsIterable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * An extension providing asynchronous collection functions
  * @author Mikko Hilpinen
  * @since 04.05.2024, v2.4
  */
object AsyncCollectionExtensions
{
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
			// Won't set up the mapping process if the original collection is empty
			if (ops.isEmpty)
				bf.fromSpecific(coll)(Iterator.empty)
			// Covers the single-threaded use-case
			else if (maxWidth <= 1)
				bf.fromSpecific(coll)(ops.map(f))
			else {
				// Also, if the original collection is small enough, simplifies the process by not limiting thread use
				val knownSize = ops.knownSize
				if (knownSize == 1)
					bf.fromSpecific(coll)(Iterator.single(f(ops.head)))
				else if (knownSize >= 0 && knownSize <= maxWidth) {
					val builder = bf.newBuilder(coll)
					ops.map { a => Future { f(a) } }.foreach { future =>
						future.waitFor() match {
							case Success(res) => builder += res
							case Failure(error) => exc.reportFailure(error)
						}
					}
					builder.result()
				}
				else {
					// Prepares the queue for parallel processing
					val queue = new ActionQueue(maxWidth)
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
								case Failure(error) => exc.reportFailure(error)
							}
						}
					builder.result()
				}
			}
		}
	}
	
	implicit def iterableOnceOperations[Repr](coll: Repr)
	                                         (implicit iter: IsIterable[Repr]): AsyncIterableOps[Repr, iter.type] =
		new AsyncIterableOps(coll, iter)
}

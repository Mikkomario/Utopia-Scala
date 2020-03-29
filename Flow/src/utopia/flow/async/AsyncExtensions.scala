package utopia.flow.async

import utopia.flow.util.{SingleWait, WaitTarget}

import scala.collection.generic.CanBuildFrom
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

/**
* This object contains extensions for asynchronous / concurrent classes
* @author Mikko Hilpinen
* @since 29.3.2019
**/
object AsyncExtensions
{
    /**
     * This implicit class provides extra features to Future
     */
	implicit class RichFuture[A](val f: Future[A]) extends AnyVal
	{
	    /**
	     * Waits for the result of this future (blocks) and returns it once it's ready
	     * @param timeout the maximum wait duration. If timeout is reached, a failure will be returned
	     * @return The result of the future. A failure if this future failed or if timeout was reached
	     */
	    def waitFor(timeout: Duration = Duration.Inf) = Try(Await.ready(f, timeout).value.get).flatten
		
		/**
		  * Creates a copy of this future that will either succeed or fail before the specified timeout duration
		  * has passed
		  * @param timeout Maximum wait duration
		  * @param exc Implicit execution context
		  * @return A future that contains the result of the original future or a failure if timeout was passed
		  *         before receiving a result.
		  */
		def withTimeout(timeout: FiniteDuration)(implicit exc: ExecutionContext) = Future { waitFor(timeout) }
		
		/**
		 * @return Whether this future is still "empty" (not completed)
		 */
		def isEmpty = !f.isCompleted
		
		/**
		  * @return Whether this future was already completed successfully
		  */
		def isSuccess = f.isCompleted && f.waitFor().isSuccess
		
		/**
		  * @return Whether this future has already failed
		  */
		def isFailure = f.isCompleted && f.waitFor().isFailure
		
		/**
		  * @return The current result of this future. None if not completed yet
		  */
		def current = if (f.isCompleted) Some(waitFor()) else None
		
		/**
		 * Makes this future "race" with another future so that only the earliest result is returned
		 * @param other Another future
		 * @param exc Execution context (implicit)
		 * @tparam B Type of return value
		 * @return A future for the first completion of these two futures
		 */
		def raceWith[B >: A](other: Future[B])(implicit exc: ExecutionContext) =
		{
			if (f.isCompleted)
				f
			else if (other.isCompleted)
				other
			else
			{
				Future {
					val resultPointer = VolatileOption[B]()
					val wait = new SingleWait(WaitTarget.UntilNotified)
					
					// Both futures try to set the pointer and end the wait
					f.foreach { r =>
						resultPointer.setOneIfEmpty(() => r)
						wait.stop()
					}
					other.foreach { r =>
						resultPointer.setOneIfEmpty(() => r)
						wait.stop()
					}
					
					// Waits until either future completes
					wait.run()
					resultPointer.get.get // Can call get because pointer is always set before wait is stopped
				}
			}
		}
	}
	
	implicit class TryFuture[A](val f: Future[Try[A]]) extends AnyVal
	{
		/**
		  * Waits for the result of this future (blocks) and returns it once it's ready
		  * @param timeout the maximum wait duration. If timeout is reached, a failure will be returned
		  * @return The result of the future. A failure if this future failed, if timeout was reached or if result was a failure
		  */
		def waitForResult(timeout: Duration = Duration.Inf): Try[A] = f.waitFor(timeout).flatten
		
		/**
		  * Creates a copy of this future with specified timeout. The resulting future will contain a failure if result
		  * wasn't received within timeout duration
		  * @param timeout A timeout duration
		  * @param exc Implicit execution context
		  * @return A future that will contain a failure if result is not received within timeout duration (the future will also
		  *         contain a failure if this future received a failure result)
		  */
		def resultWithTimeout(timeout: FiniteDuration)(implicit exc: ExecutionContext) =
			if (f.isCompleted) Future.successful(waitForResult()) else Future { waitForResult(timeout) }
		
		/**
		  * @return Whether this future already contains a success result
		  */
		def containsSuccess = f.isCompleted && waitForResult().isSuccess
		
		/**
		  * @return Whether this future already contains a failure result
		  */
		def containsFailure = f.isCompleted && waitForResult().isFailure
	}
	
	implicit class ManyFutures[A](val futures: TraversableOnce[Future[A]]) extends AnyVal
	{
		/**
		  * Waits until all of the futures inside this traversable item have completed
		  * @param cbf A can build from
		  * @tparam C Resulting collection type
		  * @return The results of the waiting (each item as a try)
		  */
		def waitFor[C]()(implicit cbf: CanBuildFrom[_, Try[A], C]) =
		{
			val buffer = cbf()
			buffer ++= futures.map { _.waitFor() }
			buffer.result()
		}
		
		/**
		  * Waits until all of the futures inside this traversable item have completed
		  * @param cbf A can build from
		  * @tparam C Resulting collection type
		  * @return The successful results of the waiting (no failures will be included)
		  */
		def waitForSuccesses[C]()(implicit cbf: CanBuildFrom[_, A, C]) =
		{
			val buffer = cbf()
			buffer ++= futures.flatMap { _.waitFor().toOption }
			buffer.result()
		}
		
		/**
		  * @param context Execution context
		  * @param cbf A can build from
		  * @tparam C result collection type
		  * @return A future of the completion of all of these items. Resulting collection contains all results wrapped in try
		  */
		def future[C](implicit context: ExecutionContext, cbf: CanBuildFrom[_, Try[A], C]): Future[C] = Future { waitFor() }
		
		/**
		  * @param context Execution context
		  * @param cbf A can build from
		  * @tparam C result collection type
		  * @return A future of the completion of all of these items. Resulting collection contains only successful completions
		  */
		def futureSuccesses[C](implicit context: ExecutionContext, cbf: CanBuildFrom[_, A, C]): Future[C] = Future { waitForSuccesses() }
		
		/**
		 * @param context Execution context
		 * @return A future of the completion of all of these items. Will not check or return the results of those operations.
		 */
		def futureCompletion(implicit context: ExecutionContext) = Future { futures.foreach { _.waitFor() } }
	}
}
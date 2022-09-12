package utopia.flow.async

import utopia.flow.time.WaitTarget
import utopia.flow.util.CollectionExtensions._

import scala.collection.immutable.VectorBuilder
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

/**
* This object contains extensions for asynchronous / concurrent classes
* @author Mikko Hilpinen
* @since 29.3.2019
**/
object AsyncExtensions
{
	/**
	  * @param reason Reason for this failure
	  * @tparam A Type of success option
	  * @return A failure in asynchronous context
	  */
	def asyncFailure[A](reason: Throwable) = Future.successful(Failure[A](reason))
	
    /**
     * This implicit class provides extra features to Future
     */
	implicit class RichFuture[A](val f: Future[A]) extends AnyVal
	{
		// TODO: Add interruptible waiting
		
	    /**
	     * Waits for the result of this future (blocks) and returns it once it's ready
	     * @param timeout the maximum wait duration. If timeout is reached, a failure will be returned
	     * @return The result of the future. A failure if this future failed or if timeout was reached
	     */
	    def waitFor(timeout: Duration = Duration.Inf) = Try { Await.ready(f, timeout).value.get }.flatten
		
		/**
		  * Creates a copy of this future that will either succeed or fail before the specified timeout duration
		  * has passed
		  * @param timeout Maximum wait duration
		  * @param exc Implicit execution context
		  * @return A future that contains the result of the original future or a failure if timeout was passed
		  *         before receiving a result.
		  */
		def withTimeout(timeout: FiniteDuration)(implicit exc: ExecutionContext) =
			Future { waitFor(timeout) }
		
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
		  * @return Current result of this future, if completed and successful. None otherwise.
		  */
		def currentSuccess = current.flatMap { _.toOption }
		
		/**
		 * Makes this future "race" with another future so that only the earliest result is returned
		 * @param other Another future
		 * @param exc Execution context (implicit)
		 * @tparam B Type of return value
		 * @return A future for the first completion of these two futures
		 */
		def raceWith[B >: A](other: Future[B])(implicit exc: ExecutionContext) = {
			if (f.isCompleted)
				f
			else if (other.isCompleted)
				other
			else {
				Future {
					val resultPointer = VolatileOption[B]()
					val wait = new Wait(WaitTarget.UntilNotified)
					
					// Both futures try to set the pointer and end the wait
					f.foreach { r =>
						resultPointer.setOneIfEmpty(r)
						wait.stop()
					}
					other.foreach { r =>
						resultPointer.setOneIfEmpty(r)
						wait.stop()
					}
					
					// Waits until either future completes
					wait.run()
					resultPointer.value.getOrElse {
						throw new InterruptedException("Wait was interrupted before either future resolved")
					}
				}
			}
		}
		
		/**
		  * @param another Another future
		  * @param exc Implicit execution context
		  * @tparam U Type of the other future
		  * @return This future, but delayed until the other future has completed
		  */
		def notCompletingBefore[U](another: Future[U])(implicit exc: ExecutionContext) =
		{
			// If the other future is already completed, doesn't need to wait for it
			if (another.isCompleted)
				f
			else
				f.zipWith(another) { (result, _) => result }
		}
	}
	
	implicit class TryFuture[A](val f: Future[Try[A]]) extends AnyVal
	{
		// TODO: Add a variation of currentSuccess
		
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
		
		/**
		  * Performs a mapping operation on a successful asynchronous result
		  * @param map A mapping function
		  * @param exc Implicit execution context
		  * @tparam B Type of map result
		  * @return A mapped version of this future
		  */
		def mapIfSuccess[B](map: A => B)(implicit exc: ExecutionContext) = f.map { r => r.map(map) }
		
		/**
		  * If this future yields a successful result, maps that with a mapping function that may fail
		  * @param map A mapping function for successful result. May fail.
		  * @param exc Implicit execution context.
		  * @tparam B Type of mapping result.
		  * @return A mapped version of this future.
		  */
		def tryMapIfSuccess[B](map: A => Try[B])(implicit exc: ExecutionContext) =
			f.map { r => r.flatMap(map) }
		
		/**
		  * If this future yields a successful result, maps it with an asynchronous mapping function.
		  * @param map An asynchronous mapping function for successful result.
		  * @param exc Implicit execution context
		  * @tparam B Type of eventual mapping result
		  * @return A mapped version of this future
		  */
		def flatMapIfSuccess[B](map: A => Future[B])(implicit exc: ExecutionContext) = f.flatMap {
			case Success(v) => map(v).map { Success(_) }
			case Failure(e) => Future.successful(Failure(e))
		}
		
		/**
		  * If this future yields a successful result, maps it with an asynchronous mapping function that may fail
		  * @param map An asynchronous mapping function for successful result. May yield a failure.
		  * @param exc Implicit execution context
		  * @tparam B Type of eventual mapping result when successful
		  * @return A mapped version of this future
		  */
		def tryFlatMapIfSuccess[B](map: A => Future[Try[B]])(implicit exc: ExecutionContext) = f.flatMap {
			case Success(v) => map(v)
			case Failure(e) => Future.successful(Failure(e))
		}
		
		/**
		  * Calls the specified function if this future completes with a success
		  * @param f A function called for a successful result
		  * @param exc Implicit execution context
		  * @tparam U Arbitrary result type
		  */
		def foreachSuccess[U](f: A => U)(implicit exc: ExecutionContext) = this.f.foreach { _.foreach(f) }
		
		/**
		  * Calls the specified function if this future completes with a failure
		  * @param f A function called for a failure result (throwable)
		  * @param exc Implicit execution context
		  * @tparam U Arbitrary result type
		  */
		def foreachFailure[U](f: Throwable => U)(implicit exc: ExecutionContext) =
			this.f.onComplete { _.flatten.failure.foreach(f) }
		
		/**
		 * Calls the specified function when this future completes. Same as calling .onComplete and then .flatten
		 * @param f A function that handles both success and failure cases
		 * @param exc Implicit execution context
		 * @tparam U Arbitrary result type
		 */
		def foreachResult[U](f: Try[A] => U)(implicit exc: ExecutionContext) =
			this.f.onComplete { r => f(r.flatten) }
	}
	
	implicit class FutureTry[A](val t: Try[Future[Try[A]]]) extends AnyVal
	{
		/**
		 * @return This try as a future
		 */
		def flattenToFuture = t.getOrMap { error => Future.successful(Failure(error)) }
	}
	
	implicit class ManyFutures[A](val futures: IterableOnce[Future[A]]) extends AnyVal
	{
		/**
		  * Waits until all of the futures inside this Iterable item have completed
		  * @return The results of the waiting (each item as a try)
		  */
		def waitFor() =
		{
			val buffer = new VectorBuilder[Try[A]]
			buffer ++= futures.iterator.map { _.waitFor() }
			buffer.result()
		}
		
		/**
		  * Waits until all of the futures inside this Iterable item have completed
		  * @return The successful results of the waiting (no failures will be included)
		  */
		def waitForSuccesses() =
		{
			val buffer = new VectorBuilder[A]
			buffer ++= futures.iterator.flatMap { _.waitFor().toOption }
			buffer.result()
		}
		
		/**
		  * @param context Execution context
		  * @return A future of the completion of all of these items. Resulting collection contains all results wrapped in try
		  */
		def future(implicit context: ExecutionContext): Future[Vector[Try[A]]] = Future { waitFor() }
		
		/**
		  * @param context Execution context
		  * @tparam C result collection type
		  * @return A future of the completion of all of these items. Resulting collection contains only successful completions
		  */
		def futureSuccesses[C](implicit context: ExecutionContext): Future[Vector[A]] = Future { waitForSuccesses() }
		
		/**
		 * @param context Execution context
		 * @return A future of the completion of all of these items. Will not check or return the results of those operations.
		 */
		def futureCompletion(implicit context: ExecutionContext) = Future { futures.iterator.foreach { _.waitFor() } }
	}
	
	implicit class ManyTryFutures[A](val futures: IterableOnce[Future[Try[A]]]) extends AnyVal
	{
		/**
		  * Blocks until all the futures in this collection have completed. Collects results.
		  * @return Results of each future in this collection
		  */
		def waitForResult() =
		{
			val builder = new VectorBuilder[Try[A]]
			futures.iterator.foreach { builder += _.waitForResult() }
			builder.result()
		}
		
		/**
		  * @param exc Implicit execution context
		  * @return Results of all futures in this collection once they arrive
		  */
		def futureResult(implicit exc: ExecutionContext) = Future { waitForResult() }
	}
}
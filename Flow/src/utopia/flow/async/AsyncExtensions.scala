package utopia.flow.async

import utopia.flow.async.process.{Wait, WaitUtils}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.TryCatch
import utopia.flow.view.mutable.async.Volatile

import scala.collection.immutable.VectorBuilder
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
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
	    /**
	     * Waits for the result of this future (blocks) and returns it once it's ready
	     * @param timeout the maximum wait duration. If timeout is reached, a failure will be returned
	     * @return The result of the future. A failure if this future failed or if timeout was reached
	     */
	    def waitFor(timeout: Duration = Duration.Inf) = Try { Await.result(f, timeout) }
		/**
		  * Blocks and waits for the result of this future.
		  * Terminates on one of 3 conditions, whichever occurs first:
		  * 1) Future resolves
		  * 2) The specified timeout is reached (if specified)
		  * 3) The specified wait lock is notified
		  * @param waitLock Wait lock that is listened upon
		  * @param timeout Maximum wait time (default = infinite)
		  * @param exc Implicit execution context
		  * @return Success if this future resolved successfully before the timeout was reached
		  *         and before the wait lock was notified. Failure otherwise.
		  */
		def waitWith(waitLock: AnyRef, timeout: Duration = Duration.Inf)(implicit exc: ExecutionContext) = {
			// If already completed, returns with completion value
			f.value.getOrElse {
				// If not, diverges into two paths:
				// 1) Natural completion
				// 2) Timeout completion, which may be triggered earlier if the waitLock is notified
				val promise = Promise[A]()
				Future {
					// If completes naturally, also terminates the timeout wait
					if (promise.tryComplete(waitFor(timeout)))
						WaitUtils.notify(waitLock)
				}
				Future {
					Wait.untilNotifiedWith(waitLock)
					promise.tryFailure(new InterruptedException("Wait interrupted"))
				}
				// Returns whichever result is acquired first (blocks)
				promise.future.waitFor(timeout)
			}
		}
		
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
		def currentResult = if (f.isCompleted) Some(waitFor()) else None
		/**
		  * @return Current result of this future, if completed and successful. None otherwise.
		  */
		def current = currentResult.flatMap { _.toOption }
		
		/**
		  * @param other Another future
		  * @param exc Implicit execution context
		  * @tparam B Type of the result in the other future
		  * @return Future that resolves once either of these futures complete successfully.
		  *         If the result is acquired from this future, it is Left, otherwise it is Right.
		  *         If both futures fail, this resulting future yields a failure, also.
		  */
		def or[B](other: => Future[B])(implicit exc: ExecutionContext) = {
			lazy val o = other
			// Case: Left future already completed => Returns it
			if (f.isSuccess)
				f.map { Left(_) }
			// Case: Right future already completed => Returns it
			else if (o.isSuccess)
				o.map { Right(_) }
			// Case: Neither future completed yet => Waits
			else
				_mergeWith(o) { (left, right) =>
					left match {
						case Some(leftResult) =>
							leftResult match {
								// Case: Left future succeeded => Returns left
								case Success(left) => Some(Left(left))
								case Failure(error) =>
									right.map {
										// Case: Left future failed but right succeeded => Returns right
										case Success(right) => Right(right)
										// Case: Both futures failed => Throws (i.e. yields a failed future)
										case Failure(_) => throw error
									}
							}
						case None =>
							right match {
								// Case: Right future succeeded => Returns right
								case Some(Success(right)) => Some(Right(right))
								// Case: Left future pending while right future pending or failed => Waits longer
								case _ => None
							}
					}
				}
		}
		/**
		 * Makes this future "race" with another future so that only the earliest result is returned
		 * @param other Another future
		 * @param exc Execution context (implicit)
		 * @tparam B Type of return value
		 * @return A future for the first completion of these two futures.
		  *         The resulting future will fail if both of these futures fail.
		 */
		def raceWith[B >: A](other: => Future[B])(implicit exc: ExecutionContext): Future[B] = {
			lazy val o = other
			// Case: This already completed => Returns this
			if (f.isSuccess)
				f
			// Case: Other already completed => Returns the other
			else if (o.isSuccess)
				o
			// Case: Neither completed => Waits
			else {
				_mergeWith(o) { (a, b) =>
					a match {
						case Some(resultA) =>
							resultA match {
								// Case: This succeeded => Returns this
								case Success(a) => Some(a)
								case Failure(error) =>
									b.map {
										// Case: This failed but other succeeded => Returns other
										case Success(b) => b
										// Case: Both failed => Throws
										case Failure(_) => throw error
									}
							}
						case None =>
							b match {
								// Case: Other succeeded => Returns other
								case Some(Success(b)) => Some(b)
								case _ => None
							}
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
		def notCompletingBefore[U](another: Future[U])(implicit exc: ExecutionContext) = {
			// If the other future is already completed, doesn't need to wait for it
			if (another.isCompleted)
				f
			else
				f.zipWith(another) { (result, _) => result }
		}
		
		// Assumes that neither of these futures has completed yet (handle those cases separately)
		// tryJoin should throw if the resulting future should fail
		private def _mergeWith[B, R](other: Future[B])(tryJoin: (Option[Try[A]], Option[Try[B]]) => Option[R])
		                            (implicit exc: ExecutionContext) =
		{
			// Pointer that collects the results of both futures, once they arrive
			val resultsPointer = Volatile[(Option[Try[A]], Option[Try[B]])](None -> None)
			// Completes the pointer asynchronously
			f.onComplete { result1 =>
				resultsPointer.update { case (_, otherResult) => Some(result1) -> otherResult }
			}
			other.onComplete { result2 =>
				resultsPointer.update { case (otherResult, _) => otherResult -> Some(result2) }
			}
			
			// Completes the future once either future successfully completes, or once both have failed
			resultsPointer.findMapFuture { case (left, right) => tryJoin(left, right) }
		}
	}
	
	implicit class FutureTry[A](val f: Future[Try[A]]) extends AnyVal
	{
		/**
		  * @return The current result of this future, but only if successful.
		  *         I.e. Only returns a value if:
		  *         a) This future is completed successfully AND
		  *         b) This future contains a success
		  */
		def currentSuccess = f.current.flatMap { _.toOption }
		/**
		  * @return The current result of this future, but only if failure.
		  *         Returns a failure if:
		  *         a) This future is completed AND
		  *         b) This future failed or contains a failure
		  */
		def currentFailure = f.currentResult.flatMap {
			case Success(result) => result.failure
			case Failure(error) => Some(error)
		}
		
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
	
	implicit class FutureTryCatch[A](val f: Future[TryCatch[A]]) extends AnyVal
	{
		/**
		 * Waits for the result of this future (blocks) and returns it once it's ready
		 * @param timeout the maximum wait duration. If timeout is reached, a failure will be returned
		 * @return The result of the future. A failure if this future failed,
		 *         if timeout was reached or if result was a failure
		 */
		def waitForResult(timeout: Duration = Duration.Inf): TryCatch[A] = f.waitFor(timeout).flattenCatching
	}
	
	implicit class TryFutureTry[A](val t: Try[Future[Try[A]]]) extends AnyVal
	{
		/**
		 * @return This try as a future
		 */
		def flattenToFuture = t.getOrMap { error => Future.successful(Failure(error)) }
	}
	
	implicit class TryFutureTryCatch[A](val t: Try[Future[TryCatch[A]]]) extends AnyVal
	{
		/**
		 * @return This try as a future
		 */
		def flattenToFuture = t.getOrMap { error => Future.successful(TryCatch.Failure(error)) }
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
		def waitForResult() = Vector.from(futures.iterator.map { _.waitForResult() })
		
		/**
		  * @param exc Implicit execution context
		  * @return Results of all futures in this collection once they arrive
		  */
		def futureResult(implicit exc: ExecutionContext) = Future { waitForResult() }
	}
	
	implicit class CompletedAttempt[A](val t: Try[A]) extends AnyVal
	{
		/**
		  * @return A resolved future (successful or failed) that contains the result of this Try
		  */
		def toCompletedFuture = t match {
			case Success(result) => Future.successful(result)
			case Failure(e) => Future.failed(e)
		}
	}
}
package utopia.flow.async

import utopia.flow.async.process.{Wait, WaitUtils}
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Single}
import utopia.flow.collection.mutable.builder.TryCatchBuilder
import utopia.flow.operator.MaybeEmpty
import utopia.flow.time.Duration
import utopia.flow.util.logging.SysErrLogger
import utopia.flow.util.result.MayHaveFailed.AlwaysSuccess
import utopia.flow.util.result.TryExtensions._
import utopia.flow.util.result.{MayHaveFailed, PossiblyFailingFuture, PossiblyFailingFutures, TryCatch}
import utopia.flow.view.mutable.async.Volatile

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
	// OTHER    ----------------------------
	
	/**
	  * @param reason Reason for this failure
	  * @tparam A Type of success option
	  * @return A failure in asynchronous context
	  */
	@deprecated("Please use TryFuture.failed(Throwable) instead", "v2.8")
	def asyncFailure[A](reason: Throwable) = Future.successful(Failure[A](reason))
	
	
	// EXTENSIONS   ------------------------
	
	/**
	 * Used for providing [[PossiblyFailingFuture]] functions for all futures,
	 * without conflicting with the methods defined for Try & TryCatch -use-cases.
	 * @tparam A Type of the successfully acquired values
	 */
	trait FutureSuccess[A] extends Any with PossiblyFailingFuture[A, A, Try]
	{
		override protected def wrap(result: A): MayHaveFailed[A] = AlwaysSuccess(result)
		override protected def unwrap[B](result: MayHaveFailed[B]): Try[B] = result.toTry
		
		override protected def failure[B](cause: Throwable): Try[B] = Failure(cause)
		
		override protected def flatten(result: Try[A]): Try[A] = result
		override protected def merge[B](result1: A, value: A, result2: MayHaveFailed[B]): Try[B] = result2.toTry
		
		override protected def resultToFuture[B](result: MayHaveFailed[Future[B]])
		                                        (implicit exc: ExecutionContext): Future[Try[B]] =
			result.toTry match {
				case Success(future) => future.map(Success.apply)
				case Failure(error) => TryFuture.failure(error)
			}
	}
	/**
	 * Used for providing [[PossiblyFailingFutures]] functions for all futures,
	 * without conflicting with the Try & TryCatch -use-cases.
	 * @tparam A Type of the values acquired on success
	 */
	trait FutureSuccesses[A] extends Any with PossiblyFailingFutures[A, A]
	{
		override protected def wrap(result: A): MayHaveFailed[A] = AlwaysSuccess(result)
		
		override protected def appendTo(builder: TryCatchBuilder[A, _], result: Try[A]): Unit = builder += result
	}
	
	implicit class RichFuture[A](override val wrapped: Future[A])
		extends AnyVal with MaybeEmpty[Future[A]] with FutureSuccess[A]
	{
		// COMPUTED -----------------------
		
		/**
		 * @return This future, if completed. None otherwise.
		 */
		def ifCompleted = if (wrapped.isCompleted) Some(wrapped) else None
		
		/**
		 * @return Returns either:
		 *              - None, if this future hasn't resolved yet
		 *              - Failure, if this future had failed
		 *              - Success, if this future had already resolved successfully
		 * @see [[currentResult]]
		 */
		def current = if (wrapped.isCompleted) Some(waitFor()) else None
		
		/**
		 * @return Whether this future was already completed successfully
		 */
		@deprecated("Deprecated for removal.", "v2.8")
		def isSuccess = wrapped.isCompleted && wrapped.waitFor().isSuccess
		/**
		 * @return Whether this future has already failed
		 */
		@deprecated("Deprecated for removal.", "v2.8")
		def isFailure = wrapped.isCompleted && wrapped.waitFor().isFailure
			
		
		// IMPLEMENTED  ----------------------
		
		override def self: Future[A] = wrapped
		
		/**
		 * @return Whether this future is still "empty" (i.e. not completed)
		 */
		override def isEmpty = !wrapped.isCompleted
		
		
		// OTHER    --------------------------
		
		/**
		 * Waits for this future to complete
		 * @param timeout Maximum wait time
		 * @return Whether this future resolved before 'timeout' was reached
		 */
		def waitUntil(timeout: Duration = Duration.infinite) =
			if (wrapped.isCompleted) true else waitFor(timeout).isSuccess
	    /**
	     * Waits for the result of this future (blocks) and returns it once it's ready
	     * @param timeout the maximum wait duration. If timeout is reached, a failure will be returned
	     * @return The result of the future. A failure if this future failed or if timeout was reached
	     */
	    def waitFor(timeout: Duration = Duration.infinite) = Try { Await.result(wrapped, timeout.toScala) }
		/**
		  * Blocks and waits for the result of this future.
		  * Terminates on one of 3 conditions, whichever occurs first:
		  *     1. Future resolves
		  *     1. The specified timeout is reached (if specified)
		  *     1. The specified wait lock is notified
		  * @param waitLock Wait lock that is listened upon
		  * @param timeout Maximum wait time (default = infinite)
		  * @param exc Implicit execution context
		  * @return Success if this future resolved successfully before the timeout was reached
		  *         and before the wait lock was notified. Failure otherwise.
		  */
		def waitWith(waitLock: AnyRef, timeout: Duration = Duration.infinite)(implicit exc: ExecutionContext) = {
			// If already completed, returns with completion value
			wrapped.value.getOrElse {
				// If not, diverges into two paths:
				//      1) Natural completion
				//      2) Timeout completion, which may be triggered earlier if the waitLock is notified
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
		  * @param other Another future
		  * @param exc Implicit execution context
		  * @tparam B Type of the result in the other future
		  * @return Future that resolves once either of these futures complete successfully.
		  *         If the result is acquired from this future, it is Left, otherwise it is Right.
		  *         If both futures fail, this resulting future yields a failure, also.
		  */
		// TODO: We probably need to either remove or refactor this
		def or[B](other: => Future[B])(implicit exc: ExecutionContext) = {
			lazy val o = other
			// Case: Left future already completed => Returns it
			if (wrapped.hasSucceeded)
				wrapped.map { Left(_) }
			// Case: Right future already completed => Returns it
			else if (o.hasSucceeded)
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
			// Case: This already completed => Returns this
			if (wrapped.hasSucceeded)
				wrapped
			else {
				val _other = other
				// Case: Other already completed => Returns the other
				if (_other.hasSucceeded)
					_other
				// Case: Neither completed => Waits
				else
					_mergeWith(_other) { (a, b) =>
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
									// Case: Neither completed yet
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
				wrapped
			else
				wrapped.zipWith(another) { (result, _) => result }
		}
		
		// Assumes that neither of these futures has completed yet (handle those cases separately)
		// tryJoin should throw if the resulting future should fail
		private def _mergeWith[B, R](other: Future[B])(tryJoin: (Option[Try[A]], Option[Try[B]]) => Option[R])
		                            (implicit exc: ExecutionContext) =
		{
			// Pointer that collects the results of both futures, once they arrive
			val resultsPointer = Volatile.eventful[(Option[Try[A]], Option[Try[B]])](None -> None)(SysErrLogger)
			// Completes the pointer asynchronously
			wrapped.onComplete { result1 =>
				resultsPointer.update { case (_, otherResult) => Some(result1) -> otherResult }
			}
			other.onComplete { result2 =>
				resultsPointer.update { case (otherResult, _) => otherResult -> Some(result2) }
			}
			
			// Completes the future once either future successfully completes, or once both have failed
			resultsPointer.findMapFuture { case (left, right) => tryJoin(left, right) }
		}
	}
	
	implicit class FutureTry[A](override val wrapped: Future[Try[A]])
		extends AnyVal with PossiblyFailingFuture[A, Try[A], Try]
	{
		// COMPUTED -------------------------
		
		/**
		  * @return The current result of this future, but only if successful.
		  *         I.e. Only returns a value if:
		  *         a) This future is completed successfully AND
		  *         b) This future contains a success
		  */
		@deprecated("Deprecated for removal", "v2.8")
		def currentSuccess = wrapped.current.flatMap { _.toOption.flatMap { _.toOption } }
		/**
		  * @return The current result of this future, but only if failure.
		  *         Returns a failure if:
		  *         a) This future is completed AND
		  *         b) This future failed or contains a failure
		  */
		@deprecated("Deprecated for removal", "v2.8")
		def currentFailure = wrapped.current.flatMap {
			case Success(result) => result.failure
			case Failure(error) => Some(error)
		}
		
		/**
		 * @return Whether this future already contains a success result
		 */
		@deprecated("Renamed to .hasSucceeded", "v2.8")
		def containsSuccess = wrapped.isCompleted && waitForResult().isSuccess
		/**
		 * @return Whether this future already contains a failure result
		 */
		@deprecated("Renamed to .hasFailed", "v2.8")
		def containsFailure = wrapped.isCompleted && waitForResult().isFailure
		
		
		// IMPLEMENTED  -------------------------
		
		override protected def wrap(result: Try[A]): MayHaveFailed[A] = result
		override protected def unwrap[B](result: MayHaveFailed[B]): Try[B] = result.toTry
		
		override protected def failure[B](cause: Throwable): Try[B] = Failure(cause)
		override protected def flatten(result: Try[Try[A]]): Try[A] = result.flatten
		override protected def merge[B](result1: Try[A], value: A, result2: MayHaveFailed[B]): Try[B] = result2.toTry
		
		override protected def resultToFuture[B](result: MayHaveFailed[Future[B]])
		                                        (implicit exc: ExecutionContext): Future[Try[B]] =
			result.toTry match {
				case Success(future) => future.map(Success.apply)
				case Failure(error) => TryFuture.failure(error)
			}
		
		override def mapSuccess[B](f: A => B)(implicit exc: ExecutionContext): Future[Try[B]] = wrapped.map { _.map(f) }
		override def tryMap[B](f: A => Try[B])(implicit exc: ExecutionContext): Future[Try[B]] =
			wrapped.map { _.flatMap(f) }
		
		
		// OTHER    -----------------------------
		
		/**
		 * Calls the specified function if this future completes with a success
		 * @param f A function called for a successful result
		 * @param exc Implicit execution context
		 * @tparam U Arbitrary result type
		 */
		def forSuccess[U](f: A => U)(implicit exc: ExecutionContext) = this.wrapped.foreach { _.foreach(f) }
		@deprecated("Renamed to .forSuccess(...)", "v2.8")
		def foreachSuccess[U](f: A => U)(implicit exc: ExecutionContext) = forSuccess(f)
		
		/**
		  * Creates a copy of this future with specified timeout. The resulting future will contain a failure if result
		  * wasn't received within timeout duration
		  * @param timeout A timeout duration
		  * @param exc Implicit execution context
		  * @return A future that will contain a failure if result is not received within timeout duration (the future will also
		  *         contain a failure if this future received a failure result)
		  */
		@deprecated("Deprecated for removal. Replaced with withTimeout(Duration).", "v2.8")
		def resultWithTimeout(timeout: Duration)(implicit exc: ExecutionContext) =
			if (wrapped.isCompleted) Future.successful(waitForResult()) else Future { waitForResult(timeout) }
		
		/**
		  * Performs a mapping operation on a successful asynchronous result
		  * @param map A mapping function
		  * @param exc Implicit execution context
		  * @tparam B Type of map result
		  * @return A mapped version of this future
		  */
		@deprecated("Renamed to .mapSuccess(...)", "v2.8")
		def mapIfSuccess[B](map: A => B)(implicit exc: ExecutionContext) = wrapped.map { r => r.map(map) }
		/**
		  * If this future yields a successful result, maps that with a mapping function that may fail
		  * @param map A mapping function for successful result. May fail.
		  * @param exc Implicit execution context.
		  * @tparam B Type of mapping result.
		  * @return A mapped version of this future.
		  */
		@deprecated("Renamed to .tryMap(...)", "v2.8")
		def tryMapIfSuccess[B](map: A => Try[B])(implicit exc: ExecutionContext) =
			wrapped.map { r => r.flatMap(map) }
		/**
		  * If this future yields a successful result, maps it with an asynchronous mapping function.
		  * @param map An asynchronous mapping function for successful result.
		  * @param exc Implicit execution context
		  * @tparam B Type of eventual mapping result
		  * @return A mapped version of this future
		  */
		@deprecated("Renamed to .flatMapSuccess(...)", "v2.8")
		def flatMapIfSuccess[B](map: A => Future[B])(implicit exc: ExecutionContext) = wrapped.flatMap {
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
		@deprecated("Renamed to .tryFlatMap(...)", "v2.8")
		def tryFlatMapIfSuccess[B](map: A => Future[Try[B]])(implicit exc: ExecutionContext) = wrapped.flatMap {
			case Success(v) => map(v)
			case Failure(e) => Future.successful(Failure(e))
		}
		
		/**
		  * Calls the specified function if this future completes with a failure
		  * @param f A function called for a failure result (throwable)
		  * @param exc Implicit execution context
		  * @tparam U Arbitrary result type
		  */
		@deprecated("Renamed to .forFailure(...)", "v2.8")
		def foreachFailure[U](f: Throwable => U)(implicit exc: ExecutionContext) =
			this.wrapped.onComplete { _.flatten.failure.foreach(f) }
		/**
		 * Calls the specified function when this future completes. Same as calling .onComplete and then .flatten
		 * @param f A function that handles both success and failure cases
		 * @param exc Implicit execution context
		 * @tparam U Arbitrary result type
		 */
		@deprecated("Renamed to .forResult(...)", "v2.8")
		def foreachResult[U](f: Try[A] => U)(implicit exc: ExecutionContext) =
			this.wrapped.onComplete { r => f(r.flatten) }
	}
	
	implicit class FutureTryCatch[A](override val wrapped: Future[TryCatch[A]])
		extends AnyVal with PossiblyFailingFuture[A, TryCatch[A], TryCatch]
	{
		// IMPLEMENTED  -----------------------
		
		override protected def wrap(result: TryCatch[A]): MayHaveFailed[A] = result
		override protected def unwrap[B](result: MayHaveFailed[B]): TryCatch[B] = result.toTryCatch
		
		override protected def failure[B](cause: Throwable): TryCatch[B] = TryCatch.Failure(cause)
		
		override protected def flatten(result: Try[TryCatch[A]]): TryCatch[A] = result.flattenCatching
		override protected def merge[B](result1: TryCatch[A], value: A, result2: MayHaveFailed[B]): TryCatch[B] =
			result2.toTryCatch.withAdditionalFailures(result1.failures)
		
		override protected def resultToFuture[B](result: MayHaveFailed[Future[B]])
		                                        (implicit exc: ExecutionContext): Future[TryCatch[B]] =
			result.toTryCatch match {
				case TryCatch.Success(future, partialFailures) => future.map { TryCatch.Success(_, partialFailures) }
				case TryCatch.Failure(error) => Future.successful(TryCatch.Failure(error))
			}
		
		override def mapSuccess[B](f: A => B)(implicit exc: ExecutionContext): Future[TryCatch[B]] =
			wrapped.map { _.map(f) }
		override def tryMapCatching[B](f: A => TryCatch[B])(implicit exc: ExecutionContext): Future[TryCatch[B]] =
			wrapped.map { _.flatMap(f) }
		
		
		// OTHER    ---------------------------
		
		/**
		 * @param f A function to call if this future succeeds.
		 *          Receives the successful value, as well as any partial errors that were encountered.
		 * @param exc Implicit execution context
		 * @tparam U Arbitrary 'f' result type
		 */
		def forSuccess[U](f: (A, Seq[Throwable]) => U)(implicit exc: ExecutionContext) =
			wrapped.foreach {
				case TryCatch.Success(value, failures) => f(value, failures)
				case _ => ()
			}
		/**
		 * @param f A function to call for any failures encountered during this process.
		 *          Not called if no failures were encountered.
		 *          Receives the encountered failures, and a boolean flag set to true if these are partial failures.
		 * @param exc Implicit execution context
		 * @tparam U Arbitrary 'f' result type
		 */
		def forFailures[U](f: (Seq[Throwable], Boolean) => U)(implicit exc: ExecutionContext) =
			wrapped.forResult {
				case TryCatch.Success(_, failures) =>
					if (failures.nonEmpty)
						f(failures, true)
				case TryCatch.Failure(error) => f(Single(error), false)
			}
		
		/**
		 * @param map A mapping function applied if this future resolves successfully
		 * @param exc Implicit execution context
		 * @tparam B Type of mapping results
		 * @return A mapped copy of this future
		 */
		@deprecated("Renamed to .mapSuccess(...)", "v2.8")
		def mapIfSuccess[B](map: A => B)(implicit exc: ExecutionContext) = wrapped.map { _.map(map) }
		/**
		 * @param map Mapping function to apply to a success result
		 * @param exc Implicit execution context
		 * @tparam B Type of mapping results, when successful
		 * @return A copy of this future with the specified function applied to the results before resolving
		 */
		@deprecated("Renamed to .tryMapCatching(...)", "v2.8")
		def tryMapIfSuccess[B](map: A => TryCatch[B])(implicit exc: ExecutionContext) =
			wrapped.map { _.flatMap(map) }
		/**
		 * If this future resolves successfully, maps it asynchronously
		 * @param map The mapping function to apply. May yield a failure.
		 * @param exc Implicit execution context
		 * @tparam B Type of mapping results, when successful
		 * @return A future that resolves once the mapping, also, has completed
		 */
		@deprecated("Renamed to .tryFlatMapCatching(...)", "v2.8")
		def tryFlatMapIfSuccess[B](map: A => Future[TryCatch[B]])(implicit exc: ExecutionContext) =
			wrapped.flatMap {
				case TryCatch.Success(value, failures) =>
					val resultFuture = map(value)
					if (failures.nonEmpty)
						resultFuture.map { _.withAdditionalFailures(failures) }
					else
						resultFuture
					
				case TryCatch.Failure(error) => Future.successful(TryCatch.Failure(error))
			}
		
		/**
		 * Calls the specified function when this future completes. Same as calling .onComplete and then .flattenCatching
		 * @param f A function that handles both success and failure cases
		 * @param exc Implicit execution context
		 * @tparam U Arbitrary result type
		 */
		@deprecated("Renamed to .forResult(...)", "v2.8")
		def foreachResult[U](f: TryCatch[A] => U)(implicit exc: ExecutionContext) =
			this.wrapped.onComplete { r => f(r.flattenCatching) }
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
	
	implicit class ManyFutures[A](val futures: IterableOnce[Future[A]]) extends AnyVal with FutureSuccesses[A]
	{
		// COMPUTED ---------------------------
		
		/**
		 * @param context Execution context
		 * @return A future of the completion of all of these items. Resulting collection contains only successful completions
		 */
		@deprecated("Deprecated for removal. Please use .future instead", "v2.8")
		def futureSuccesses(implicit context: ExecutionContext): Future[Seq[A]] = Future { waitForSuccesses() }
		@deprecated("Deprecated for removal. Please use .completionFuture instead", "v2.8")
		def futureCompletion(implicit context: ExecutionContext) = Future { futures.iterator.foreach { _.waitFor() } }
		
		
		// IMPLEMENTED  --------------------
		
		override protected def wrapped: IterableOnce[Future[A]] = futures
		
		
		// OTHER    ------------------------
		
		/**
		  * Waits until all the futures inside this Iterable item have completed
		  * @return The successful results of the waiting (no failures will be included)
		  */
		@deprecated("Please use waitForResults() instead", "v2.8")
		def waitForSuccesses() = {
			val buffer = OptimizedIndexedSeq.newBuilder[A]
			buffer ++= futures.iterator.flatMap { _.waitFor().toOption }
			buffer.result()
		}
	}
	
	implicit class ManyTryFutures[A](override val wrapped: IterableOnce[Future[Try[A]]])
		extends AnyVal with PossiblyFailingFutures[A, Try[A]]
	{
		// IMPLEMENTED  ----------------------
		
		override protected def wrap(result: Try[A]): MayHaveFailed[A] = result
		
		override protected def appendTo(builder: TryCatchBuilder[A, _], result: Try[Try[A]]): Unit =
			builder += result.flatten
		
		
		// OTHER    --------------------------
		
		/**
		  * Blocks until all the futures in this collection have completed. Collects the results.
		  * @return Results of each future in this collection, as a [[TryCatch]].
		 *         Yields a failure if all these futures failed.
		  */
		@deprecated("Replaced with waitForResults()", "v2.8")
		def waitForResult() = wrapped.iterator.map { _.waitForResult() }.toTryCatch
	}
	
	implicit class ManyTryCatchFutures[A](override val wrapped: IterableOnce[Future[TryCatch[A]]])
		extends AnyVal with PossiblyFailingFutures[A, TryCatch[A]]
	{
		// IMPLEMENTED  ------------------------
		
		override protected def wrap(result: TryCatch[A]): MayHaveFailed[A] = result
		
		override protected def appendTo(builder: TryCatchBuilder[A, _], result: Try[TryCatch[A]]): Unit =
			builder += result.flattenCatching
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
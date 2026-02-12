package utopia.flow.util.result

import utopia.flow.async.AsyncExtensions._
import utopia.flow.time.Duration
import TryExtensions._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
 * Common trait for futures that may fail (i.e. all futures).
 * Extended directly by futures that yield Try or TryCatch.
 * Extended indirectly by other futures, so that the Try & TryCatch versions receive priority.
 * @tparam A Type of the successfully acquired values
 * @tparam T Type of the asynchronously acquired results (e.g. Try[A])
 * @tparam R Generic type of the result-wrappers (e.g. Try)
 * @author Mikko Hilpinen
 * @since 17.12.2025, v2.8
 */
trait PossiblyFailingFuture[A, T, +R[_]] extends Any
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return The wrapped/extended future
	 */
	protected def wrapped: Future[T]
	
	/**
	 * Wraps a result as a [[MayHaveFailed]]
	 * @param result Result to wrap
	 * @return A wrapped result
	 */
	protected def wrap(result: T): MayHaveFailed[A]
	/**
	 * Unwraps a [[MayHaveFailed]] back into the primary result type
	 * @param result A wrapped result
	 * @tparam B Type of the value on success
	 * @return The unwrapped result
	 */
	protected def unwrap[B](result: MayHaveFailed[B]): R[B]
	
	/**
	 * Creates a new failure result
	 * @param cause Cause of this failure
	 * @tparam B Type of the value that would have been yielded on success
	 * @return A failure result
	 */
	protected def failure[B](cause: Throwable): R[B]
	
	/**
	 * Flattens a result
	 * @param result A Try containing an asynchronously acquired result
	 * @return A flattened result
	 */
	protected def flatten(result: Try[T]): R[A]
	/**
	 * Merges two results (e.g. used for preserving/forwarding partial failures)
	 * @param result1 The first result (e.g. before mapping)
	 * @param result2 The second result (e.g. after mapping)
	 * @tparam B Type of the (mapped) value on success
	 * @return 'result2', including information from 'result1', if important and applicable
	 */
	protected def merge[B](result1: T, value: A, result2: MayHaveFailed[B]): R[B]
	
	/**
	 * Converts a possibly failed attempt to create a future, into a future result
	 * @param result Result to map
	 * @param exc Implicit execution context
	 * @tparam B Type of the future results
	 * @return A future that yields either a success or a failure.
	 *         Failed immediately, if 'result' is a failure.
	 */
	protected def resultToFuture[B](result: MayHaveFailed[Future[B]])(implicit exc: ExecutionContext): Future[R[B]]
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return The current result of this future. None if not completed yet.
	 *         Yields a failure if this future had already failed.
	 */
	def currentResult = if (wrapped.isCompleted) Some(waitForResult()) else None
	
	/**
	 * @return Whether this future already contains a success result
	 */
	def hasSucceeded = wrapped.current.exists { _.toOption.exists { wrap(_).isSuccess } }
	/**
	 * @return Whether this future already contains a failure result
	 */
	def hasFailed = wrapped.current.exists { _.toOption.forall { wrap(_).isFailure } }
	
	/**
	 * @return A copy of this future that fails if it contains a failure
	 */
	def unwrapped(implicit exc: ExecutionContext) = wrapped.map { result => wrap(result).get }
	/**
	 * @param exc Implicit execution context
	 * @return A future that will contain a Success if this future succeeded, and a Failure if this future failed.
	 */
	def toTryFuture(implicit exc: ExecutionContext) =
		wrapped.map { wrap(_).toTry }.recover { case e => Failure(e) }
	/**
	 * @param exc Implicit execution context
	 * @return A copy of this future where the underlying Try is converted into a TryCatch
	 */
	def toFutureTryCatch(implicit exc: ExecutionContext) =
		wrapped.map { result => wrap(result).toTryCatch }.recover { case e => TryCatch.Failure(e) }
	
	
	// OTHER    -------------------------
	
	/**
	 * Waits until the result of this future has resolved.
	 *
	 * Note: This may block for an extensive period of time.
	 *       It is recommended, that you use other functions, such as map, flatMap or forEachResult instead.
	 *
	 * @param timeout Maximum wait time. Default = Infinite.
	 * @return The result of this future. Failure if timeout was reached.
	 */
	def waitForResult(timeout: Duration = Duration.infinite) = flatten(wrapped.waitFor(timeout))
	
	/**
	 * Performs a mapping operation on a successful asynchronous result
	 * @param f A mapping function
	 * @param exc Implicit execution context
	 * @tparam B Type of map result
	 * @return A mapped version of this future
	 */
	def mapSuccess[B](f: A => B)(implicit exc: ExecutionContext) =
		wrapped.map { r => unwrap(wrap(r).map(f)) }
	/**
	 * If this future yields a successful result, maps it with an asynchronous mapping function.
	 * @param f An asynchronous mapping function for successful result.
	 * @param exc Implicit execution context
	 * @tparam B Type of eventual mapping result
	 * @return A mapped version of this future
	 */
	def flatMapSuccess[B](f: A => Future[B])(implicit exc: ExecutionContext) =
		wrapped.flatMap { r => resultToFuture(wrap(r).map(f)) }
	
	/**
	 * Maps the result of this future, if successful.
	 * @param f A mapping function to apply, may yield a failure.
	 * @param exc Implicit execution context
	 * @tparam B Type of the mapping results, if successful
	 * @return A mapped copy of this future
	 */
	def mapOrFail[B](f: A => MayHaveFailed[B])(implicit exc: ExecutionContext) =
		wrapped.map { result1 =>
			wrap(result1).toTry match {
				case Success(v1) => merge(result1, v1, f(v1))
				case Failure(error) => failure[B](error)
			}
		}
	/**
	 * Asynchronously maps the result of this future, if successful.
	 * @param f A mapping function to apply. Yields a future which may yield a failure.
	 * @param exc Implicit execution context
	 * @tparam B Type of the mapping results, if successful
	 * @return A future that resolves once either:
	 *              1. This future yields a failure
	 *              1. The result of 'f' resolves
	 */
	def flatMapOrFail[B](f: A => Future[MayHaveFailed[B]])(implicit exc: ExecutionContext) =
		wrapped.flatMap { result1 =>
			wrap(result1).toTry match {
				case Success(v1) => f(v1).map { merge(result1, v1, _) }
				case Failure(error) => Future.successful(failure[B](error))
			}
		}
	
	/**
	 * If this future yields a successful result, maps that with a mapping function that may fail
	 * @param f A mapping function for successful result. May fail.
	 * @param exc Implicit execution context.
	 * @tparam B Type of mapping result.
	 * @return A mapped version of this future.
	 */
	def tryMap[B](f: A => Try[B])(implicit exc: ExecutionContext) =
		wrapped.map { r => unwrap(wrap(r).tryMap(f)) }
	/**
	 * If this future yields a successful result, maps it with an asynchronous mapping function that may fail
	 * @param f An asynchronous mapping function for successful result. May yield a failure.
	 * @param exc Implicit execution context
	 * @tparam B Type of eventual mapping result when successful
	 * @return A mapped version of this future
	 */
	def tryFlatMap[B](f: A => Future[Try[B]])(implicit exc: ExecutionContext) =
		wrapped.flatMap { r1 =>
			wrap(r1).toTry match {
				case Success(value) => f(value).map { r2 => merge(r1, value, r2) }
				case Failure(error) => Future.successful(failure[B](error))
			}
		}
	
	/**
	 * @param f A mapping function applied to a failure. May yield a success or a failure.
	 * @param exc Implicit execution context
	 * @return A future that applies the specified mapping function, if this is a failure.
	 */
	def tryMapFailure(f: Throwable => Try[A])(implicit exc: ExecutionContext) =
		wrapped.map { r =>
			val result = wrap(r)
			result.failure match {
				case Some(error) => unwrap(f(error))
				case None => unwrap(result)
			}
		}
	/**
	 * @param f A mapping function applied to a failure. Yields a Future that may yield a success or a failure.
	 * @param exc Implicit execution context
	 * @return A future that applies the specified mapping function, if this is a failure.
	 */
	def tryFlatMapFailure(f: Throwable => Future[Try[A]])(implicit exc: ExecutionContext) =
		wrapped.flatMap { r =>
			val result = wrap(r)
			result.failure match {
				case Some(error) => f(error).map { unwrap(_) }
				case None => Future.successful(unwrap(result))
			}
		}
	
	/**
	 * If this future yields a successful result,
	 * maps that with a mapping function that may fail (fully or partially)
	 * @param f A mapping function for successful result. May fail.
	 * @param exc Implicit execution context.
	 * @tparam B Type of mapping result.
	 * @return A mapped version of this future.
	 */
	def tryMapCatching[B](f: A => TryCatch[B])(implicit exc: ExecutionContext) =
		wrapped.map { r => wrap(r).toTryCatch.flatMap(f) }
	/**
	 * If this future yields a successful result,
	 * maps it with an asynchronous mapping function that may fail (fully or partially)
	 * @param f An asynchronous mapping function for successful result. May yield a failure.
	 * @param exc Implicit execution context
	 * @tparam B Type of eventual mapping result when successful
	 * @return A mapped version of this future
	 */
	def tryFlatMapCatching[B](f: A => Future[TryCatch[B]])(implicit exc: ExecutionContext) =
		wrapped.flatMap { r =>
			wrap(r).toTryCatch match {
				case TryCatch.Success(value, partialFailures) =>
					val resultFuture = f(value)
					if (partialFailures.isEmpty)
						resultFuture
					else
						resultFuture.map { _.withAdditionalFailures(partialFailures) }
				
				case TryCatch.Failure(error) => Future.successful(TryCatch.Failure(error))
			}
		}
	
	/**
	 * Calls the specified function if this future completes with a failure
	 * @param f A function called for a failure result (throwable)
	 * @param exc Implicit execution context
	 * @tparam U Arbitrary result type
	 */
	def forFailure[U](f: Throwable => U)(implicit exc: ExecutionContext) = wrapped.onComplete {
		case Success(result) => wrap(result).failure.foreach(f)
		case Failure(error) => f(error)
	}
	/**
	 * Calls the specified function when this future completes. Same as calling .onComplete and then .flatten
	 * @param f A function that handles both success and failure cases
	 * @param exc Implicit execution context
	 * @tparam U Arbitrary result type
	 */
	def forResult[U](f: R[A] => U)(implicit exc: ExecutionContext) = wrapped.onComplete { r => f(flatten(r)) }
	
	/**
	 * Creates a copy of this future that will either succeed or fail before the specified timeout duration
	 * has passed
	 * @param timeout Maximum wait duration
	 * @param exc Implicit execution context
	 * @return A future that contains the result of this original future, or a failure if timeout was passed
	 *         before receiving a result.
	 */
	def withTimeout(timeout: Duration)(implicit exc: ExecutionContext) = currentResult match {
		case Some(result) => Future.successful(result)
		case None => Future { waitForResult(timeout) }
	}
	
	/*
def toEither[B](other: => Future[B])
			   (implicit exc: ExecutionContext, detectFailure: B => MayHaveFailed[_]) =
{
	lazy val _other = other
	wrapped.current match {
		case Some(Success(currentResult)) =>
			val r = wrap(currentResult)
			if (r.isSuccess)
				Some(Right(r)) -> 1
			else
			
			???
		case None =>
			other.current match {
				case Some(Success(result)) =>
					val r = detectFailure(result)
					if (r.isSuccess)
						Some(Left(r)) -> 1
					else
						None -> 1
				case Some(Failure(_)) => None -> 1
				case None => None -> 0
			}
	}
	
	if (hasSucceeded)
		Future.successful(Right(wrapped))
	else {
		if (_other.hasSucceeded)
			Future.successful(Left(_other))
		else if (wrapped.isCompleted)
			Future.successful(Right(wrapped))
		else {
			val promise = Promise[Either[B, R[A]]]()
			val failureCounter = Volatile(0)
			
			wrapped.onComplete {
				case Success(result) =>
					val r = wrap(result)
					if (r.isSuccess || failureCounter.updateAndGet { _ + 1 } == 2)
						promise.trySuccess(Right(unwrap(r)))
				
				case Failure(error) =>
					if (failureCounter.updateAndGet { _ + 1 } == 2)
						promise.success(Right(failure(error)))
			}
			_other.onComplete {
				case Success(result) =>
					val r = detectFailure(result)
					if (r.isSuccess)
						promise.trySuccess(Left(result))
					else if (failureCounter.updateAndGet { _ + 1 } == 2)
						promise.success(Left(result))
				
				case Failure(error) =>
					if (failureCounter.updateAndGet { _ + 1 } == 2)
						promise.success(Right(failure(error)))
			}
			
			promise.future
		}
	}
}*/
}
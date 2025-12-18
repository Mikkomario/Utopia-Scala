package utopia.annex.util

import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.{RequestFailure, RequestResult, Response}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.builder.TryCatchBuilder
import utopia.flow.time.Duration
import utopia.flow.util.result.{Attempts, MayHaveFailed, PossiblyFailingFuture, PossiblyFailingFutures}
import utopia.flow.util.result.TryExtensions._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Adds methods relating to request results
  * @author Mikko Hilpinen
  * @since 01.08.2024, v1.9
  */
object RequestResultExtensions
{
	implicit class RequestResults[A](val results: IterableOnce[RequestResult[A]])
		extends AnyVal with Attempts[A, RequestResult[A], RequestResult]
	{
		override protected def iterator: Iterator[RequestResult[A]] = results.iterator
		
		override protected def wrap(result: RequestResult[A]): MayHaveFailed[A] = result
		override protected def unwrap[B](result: MayHaveFailed[B]): RequestResult[B] = RequestResult.from(result)
	}
	
	implicit class AsyncRequestResult[A](val wrapped: Future[RequestResult[A]])
		extends AnyVal with PossiblyFailingFuture[A, RequestResult[A], RequestResult]
	{
		// IMPLEMENTED  -----------------------
		
		override protected def wrap(result: RequestResult[A]): MayHaveFailed[A] = result
		override protected def unwrap[B](result: MayHaveFailed[B]): RequestResult[B] = RequestResult.from(result)
		
		override protected def failure[B](cause: Throwable): RequestResult[B] = RequestSendingFailed(cause)
		override protected def flatten(result: Try[RequestResult[A]]): RequestResult[A] = result.getOrMap(failure)
		override protected def merge[B](result1: RequestResult[A], value: A,
		                                result2: MayHaveFailed[B]): RequestResult[B] =
			result2 match {
				case result: RequestResult[B] => result
				case result =>
					result.toTry match {
						case Success(value) =>
							result1 match {
								case response: Response[_] => response.toSuccessWithValue(value)
								case _ => Response.Success(value)
							}
						case Failure(error) =>
							result1 match {
								case Response.Success(_, _, headers) =>
									Response.Failure(Response.parseFailureStatus,
										Option(error.getMessage).getOrElse(""), headers)
								case Response.Failure(status, previousMessage, headers) =>
									Response.Failure(status, Option(error.getMessage).getOrElse(previousMessage),
										headers)
								case _ => RequestSendingFailed(error)
							}
					}
			}
		
		override protected def resultToFuture[B](result: MayHaveFailed[Future[B]])
		                                        (implicit exc: ExecutionContext): Future[RequestResult[B]] =
			result match {
				case Response.Success(future: Future[B], status, headers) =>
					future.map { Response.Success(_, status, headers) }
				case failure: RequestFailure => Future.successful(failure)
				case result =>
					result.toTry match {
						case Success(future) => future.map { Response.Success(_) }
						case Failure(error) => Future.successful(RequestSendingFailed(error))
					}
			}
		
		/**
		 * Maps the response body value, if successful, once it resolves
		 * @param f A mapping function applied to a successfully acquired body value
		 * @param exc Implicit execution context
		 * @tparam B Type of mapping results
		 * @return A mapped copy of this future
		 */
		override def mapSuccess[B](f: A => B)(implicit exc: ExecutionContext) =
			wrapped.map { _.map(f) }
		
		
		// OTHER    --------------------------
		
		/**
		 * Asynchronously maps the response body value, if successful, once it resolves.
		 * The results are collapsed into a Try.
		 * @param f A mapping function applied to a successfully acquired body value. Yields a future.
		 * @param exc Implicit execution context
		 * @tparam B Type of mapping results
		 * @return A mapped copy of this future, containing a Try instead of a RequestResult.
		 */
		@deprecated("Deprecated for removal. Please use .flatMapSuccess instead", "v1.12")
		def flatMapSuccessToTry[B](f: A => Future[B])(implicit exc: ExecutionContext) = {
			wrapped.flatMap { _.toTry match {
				case Success(value) => f(value).map { Success(_) }
				case Failure(error) => Future.successful(Failure(error))
			} }
		}
		
		/**
		  * Waits until the result arrives, then returns it as a Try.
		  * NB: May block for extensive periods of time
		  * @param timeout Duration after which a failure is yielded (default = infinite)
		 * @return Acquired request result, converted into a Try
		  */
		@deprecated("Deprecated for removal. Please use .waitForResult() instead", "v1.12")
		def waitForTry(timeout: Duration = Duration.infinite) = wrapped.waitFor(timeout).flatMap { _.toTry }
		
		/**
		 * Calls the specified function once this future resolves.
		 * Wraps a failed future in a RequestSendingFailed instance.
		 * @param f A function called once this future resolves
		 * @param exc Implicit execution context
		 * @tparam U Arbitrary function result type
		 */
		@deprecated("Please use .forResult(...) instead", "v1.12")
		def foreachResult[U](f: RequestResult[A] => U)(implicit exc: ExecutionContext) =
			wrapped.onComplete { result => f(result.getOrMap(RequestSendingFailed)) }
	}
	
	implicit class AsyncRequestResults[A](override val wrapped: IterableOnce[Future[RequestResult[A]]])
		extends AnyVal with PossiblyFailingFutures[A, RequestResult[A]]
	{
		override protected def wrap(result: RequestResult[A]): MayHaveFailed[A] = result
		
		override protected def appendTo(builder: TryCatchBuilder[A, _], result: Try[RequestResult[A]]): Unit =
			result match {
				case Success(result) => builder += result.toTry
				case Failure(error) => builder += error
			}
	}
	
	implicit class AsyncRequestMultiResult[+A](val wrapped: Future[RequestResult[Seq[A]]]) extends AnyVal
	{
		/**
		  * Maps each value in the response body, if successful, once it resolves
		  * @param f A mapping function applied to successfully acquired body values
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future
		  */
		def mapEach[B](f: A => B)(implicit exc: ExecutionContext) =
			wrapped.map { _.map { _.map(f) } }
		/**
		  * Maps each value in the response body, if successful, once it resolves
		  * @param f A mapping function applied to successfully acquired body values.
		  *          Yields 0-n items per body value.
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future
		  */
		def flatMapEach[B](f: A => IterableOnce[B])(implicit exc: ExecutionContext) =
			wrapped.map { _.map { _.flatMap(f) } }
		/**
		  * Maps each value in the response body, if successful, once it resolves.
		  * Collapses the result into a Try.
		  * @param f A mapping function applied to successfully acquired body values. May yield a failure.
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future. Contains a failure if any of the mapping functions failed.
		  */
		def tryMapEach[B](f: A => Try[B])(implicit exc: ExecutionContext) =
			wrapped.map { _.tryMap { _.tryMap(f) } }
	}
	
	implicit class AsyncOptionalRequestResult[+A](val wrapped: Future[RequestResult[Option[A]]]) extends AnyVal
	{
		/**
		  * Maps the response body value, if successful and not empty, once it resolves.
		  * Empty body values are preserved as None.
		  * @param f A mapping function applied to a successfully acquired body value
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future
		  */
		def mapIfNotEmpty[B](f: A => B)(implicit exc: ExecutionContext) =
			wrapped.map { _.map { _.map(f) } }
		/**
		  * Maps the response body value, if successful and not empty, once it resolves.
		  * The results are collapsed into a Try. Empty body values are preserved as None.
		  * @param f A mapping function applied to a successfully acquired non-empty body value. May fail.
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future. Contains a (parsing) failure if 'f' yielded a failure.
		  */
		def tryMapIfNotEmpty[B](f: A => Try[B])(implicit exc: ExecutionContext) =
			wrapped.map { result =>
				result.tryMap {
					case Some(value) => f(value).map(Some.apply)
					case None => Success(None)
				}
			}
	}
}

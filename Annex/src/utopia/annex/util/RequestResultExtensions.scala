package utopia.annex.util

import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.{RequestResult, Response}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

/**
  * Adds methods relating to request results
  * @author Mikko Hilpinen
  * @since 01.08.2024, v1.9
  */
object RequestResultExtensions
{
	implicit class AsyncRequestResult[+A](val f: Future[RequestResult[A]]) extends AnyVal
	{
		/**
		  * Waits until the result arrives, then returns it.
		  * NB: May block for extensive periods of time
		  * @return Acquired request result
		  */
		def waitForResult() = f.waitFor().getOrMap(RequestSendingFailed)
		/**
		  * Waits until the result arrives, then returns it as a Try.
		  * NB: May block for extensive periods of time
		  * @return Acquired request result, converted into a Try
		  */
		def waitForTry() = f.waitFor().flatMap { _.toTry }
		
		/**
		  * Calls the specified function once this future resolves.
		  * Wraps a failed future in a RequestSendingFailed instance.
		  * @param f A function called once this future resolves
		  * @param exc Implicit execution context
		  * @tparam U Arbitrary function result type
		  */
		def foreachResult[U](f: RequestResult[A] => U)(implicit exc: ExecutionContext) =
			this.f.onComplete { result => f(result.getOrMap(RequestSendingFailed)) }
		
		/**
		  * Maps the response body value, if successful, once it resolves
		  * @param f A mapping function applied to a successfully acquired body value
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future
		  */
		def mapSuccess[B](f: A => B)(implicit exc: ExecutionContext) =
			this.f.map { _.map(f) }
		/**
		  * Maps the response body value, if successful, once it resolves.
		  * The results are collapsed into a Try.
		  * @param f A mapping function applied to a successfully acquired body value.
		  *          May fail.
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future, containing a Try instead of a RequestResult.
		  */
		def tryMapSuccess[B](f: A => Try[B])(implicit exc: ExecutionContext) =
			this.f.map { _.toTry.flatMap(f) }
		/**
		 * Maps the response body value, if successful, once it resolves.
		 * The results are collapsed into a Try.
		 * @param f A mapping function applied to a successfully acquired body value.
		 *          Yields a future. May yield a failure.
		 * @param exc Implicit execution context
		 * @tparam B Type of mapping results
		 * @return A mapped copy of this future, containing a Try instead of a RequestResult.
		 */
		def tryFlatMapSuccess[B](f: A => Future[Try[B]])(implicit exc: ExecutionContext) =
			this.f.flatMap { _.toTry.map(f).flattenToFuture }
	}
	
	implicit class AsyncRequestMultiResult[+A](val f: Future[RequestResult[Seq[A]]]) extends AnyVal
	{
		/**
		  * Maps each value in the response body, if successful, once it resolves
		  * @param f A mapping function applied to successfully acquired body values
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future
		  */
		def mapEach[B](f: A => B)(implicit exc: ExecutionContext) =
			this.f.map { _.map { _.map(f) } }
		/**
		  * Maps each value in the response body, if successful, once it resolves
		  * @param f A mapping function applied to successfully acquired body values.
		  *          Yields 0-n items per body value.
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future
		  */
		def flatMapEach[B](f: A => IterableOnce[B])(implicit exc: ExecutionContext) =
			this.f.map { _.map { _.flatMap(f) } }
		/**
		  * Maps each value in the response body, if successful, once it resolves.
		  * Collapses the result into a Try.
		  * @param f A mapping function applied to successfully acquired body values.
		  *          May yield a failure. The result will be converted to a failure if any mapping fails.
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future, containing a Try instead of a RequestResult.
		  */
		def tryMapEach[B](f: A => Try[B])(implicit exc: ExecutionContext) =
			this.f.map { _.toTry.flatMap { _.tryMap(f) } }
	}
	
	implicit class AsyncOptionalRequestResult[+A](val f: Future[RequestResult[Option[A]]]) extends AnyVal
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
			this.f.map { _.map { _.map(f) } }
		/**
		  * Maps the response body value, if successful and not empty, once it resolves.
		  * The results are collapsed into a Try. Empty body values are preserved as None.
		  * @param f A mapping function applied to a successfully acquired non-empty body value.
		  *          May fail.
		  * @param exc Implicit execution context
		  * @tparam B Type of mapping results
		  * @return A mapped copy of this future, containing a Try instead of a RequestResult.
		  */
		def tryMapIfNotEmpty[B](f: A => Try[B])(implicit exc: ExecutionContext) =
			this.f.map { _.toTry.flatMap {
				case Some(v) => f(v).map { Some(_) }
				case None => Success(None)
			} }
	}
}

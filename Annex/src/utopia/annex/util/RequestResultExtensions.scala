package utopia.annex.util

import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.RequestResult
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Adds methods relating to request results
  * @author Mikko Hilpinen
  * @since 01.08.2024, v1.8.1
  */
object RequestResultExtensions
{
	implicit class AsyncRequestResult[A](val f: Future[RequestResult[A]]) extends AnyVal
	{
		/**
		  * Waits until the result arrives, then returns it.
		  * NB: May block for extensive periods of time
		  * @return Acquired request result
		  */
		def waitForResult() = f.waitFor().getOrMap(RequestSendingFailed)
		
		/**
		  * Calls the specified function once this future resolves.
		  * Wraps a failed future in a RequestSendingFailed instance.
		  * @param f A function called once this future resolves
		  * @param exc Implicit execution context
		  * @tparam U Arbitrary function result type
		  */
		def foreachResult[U](f: RequestResult[A] => U)(implicit exc: ExecutionContext) =
			this.f.onComplete { result => f(result.getOrMap(RequestSendingFailed)) }
	}
}

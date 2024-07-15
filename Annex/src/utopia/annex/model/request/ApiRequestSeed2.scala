package utopia.annex.model.request

import scala.concurrent.Future
import scala.util.Try

/**
  * Represents an API-request in "seed form".
  * These seeds are processed like API requests until they need to be sent.
  * At this point they're converted into actual requests. The conversion may fail.
  *
  * Using API-request seeds may be useful in situations where you wish to queue multiple consecutive
  * inter-dependent requests. If the previous request fails to generate the desired result, the following
  * requests can then be retracted before they are fully converted into a request format.
  *
  * Request seeds are always persisting, because non-persisting use-case is better handled with simple Future chaining.
  * This trait facilitates the much more complex use-case where request chains need to be persisted.
  *
  * @tparam A type of the parsed request response body
  * @author Mikko Hilpinen
  * @since 21.12.2023, v1.7
  */
trait ApiRequestSeed2[+A] extends Persisting with Retractable
{
	/**
	  * Converts this seed into an API-request, if possible.
	  * This function should only be called for non-deprecated seeds, and preferable as late as possible.
	  * @return A future that resolves into an API-request to send, or a failure,
	  *         in which case the sending is cancelled.
	  */
	def toRequest: Future[Try[ApiRequest2[A]]]
}

package utopia.disciple.controller

import utopia.disciple.http.request.Request
import utopia.disciple.http.response.StreamedResponse

import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object ResponseInterceptor
{
	// OTHER    ----------------------------
	
	/**
	  * Wraps a function into an interceptor instance
	  * @param f A function which receives received response (or a failure to acquire a response) and the request
	  *          which yielded that response.
	  *          Yields a potentially modified copy of the response to relay further.
	  * @return A new interceptor that utilizes the specified function.
	  */
	implicit def apply(f: (Try[StreamedResponse], Request) => Try[StreamedResponse]): ResponseInterceptor =
		new InterceptorFunctionWrapper(f)
	
	/**
	  * Wraps a function into an interceptor that only intercepts failures to acquire a response
	  * @param f A function that accepts the cause of the failure, as well as the failed request.
	  *          Returns either a successful response replica or a failure.
	  * @return A new interceptor based on the specified function.
	  */
	def forFailures(f: (Throwable, Request) => Try[StreamedResponse]) =
		apply { (res, req) =>
			res match {
				case s: Success[StreamedResponse] => s
				case Failure(error) => f(error, req)
			}
		}
	/**
	  * Wraps a function into an interceptor that only handles received responses
	  * (i.e. not failures to acquire a response)
	  * @param f A function which accepts the received response and the request which yielded that response.
	  *          Returns either a success with a possibly modified response, or a failure.
	  * @return A new interceptor based on the specified function
	  */
	def forSuccesses(f: (StreamedResponse, Request) => Try[StreamedResponse]) =
		apply { (res, req) =>
			res match {
				case Success(res) => f(res, req)
				case f: Failure[StreamedResponse] => f
			}
		}
	
	/**
	  * Wraps a function into a non-modifying response interceptor
	  * @param f A function which receives received response (or a failure to acquire a response) and the request
	  *          which yielded that response.
	  * @tparam U Arbitrary function result type
	  * @return A new interceptor that utilizes the specified function.
	  */
	def readOnly[U](f: (Try[StreamedResponse], Request) => U) =
		apply { (res, req) => { f(res, req); res } }
	
	
	// NESTED   ----------------------------
	
	private class InterceptorFunctionWrapper(f: (Try[StreamedResponse], Request) => Try[StreamedResponse])
		extends ResponseInterceptor
	{
		override def intercept(response: Try[StreamedResponse], request: Request): Try[StreamedResponse] =
			f(response, request)
	}
}

/**
  * Response interceptors are the first instances to gain access to incoming responses,
  * and may be used for modifying the responses before they're sent further
  * @author Mikko Hilpinen
  * @since 19.1.2023, v1.5.4
  */
trait ResponseInterceptor
{
	/**
	  * Intercepts an incoming response (or a failure to acquire a response)
	  * @param response Received response. Failure if no response was received.
	  * @param request Request that yielded the specified response.
	  * @return A possibly modified copy of the received response,
	  *         which will be forwarded to the recipient in place of the original.
	  */
	def intercept(response: Try[StreamedResponse], request: Request): Try[StreamedResponse]
}

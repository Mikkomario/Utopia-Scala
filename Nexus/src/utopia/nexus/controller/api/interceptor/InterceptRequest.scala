package utopia.nexus.controller.api.interceptor

import utopia.nexus.model.request.Request

object InterceptRequest
{
	// OTHER    -------------------------
	
	/**
	 * Creates a request interception logic that makes no modifications
	 * @param f A function which receives all intercepted requests
	 * @tparam C Type of applied context
	 * @return A new request interception logic
	 */
	def readOnly[C](f: Request[Any] => Unit): InterceptRequest[C] = new ReadOnlyInterceptor[C](f)
	
	
	// NESTED   -------------------------
	
	private class ReadOnlyInterceptor[-C](f: Request[Any] => Unit) extends InterceptRequest[C]
	{
		override def intercept[B](request: Request[B]): (Request[B], Option[RequestInterceptor[C]]) = {
			f(request)
			request -> None
		}
	}
}

/**
 * Common trait for (stateless) interfaces for intercepting (and possibly modifying) requests processed by
 * [[utopia.nexus.controller.api.ApiRoot]], and the request results generated through that process.
 *
 * The interception is performed by (potentially) generating a [[RequestInterceptor]] for each processed request.
 *
 * @tparam C Type of request context used
 *
 * @author Mikko Hilpinen
 * @since 07.11.2025, v2.0
 */
trait InterceptRequest[-C]
{
	/**
	 * Intercepts a request before it is processed
	 * @param request The request to intercept
	 * @tparam B Type of request body in question
	 * @return Returns 2 values:
	 *              1. A potentially modified copy of the intercepted request
	 *              1. A [[RequestInterceptor]] instance that may intercept and adjust the
	 *                 request-handling process at its various phases
	 */
	def intercept[B](request: Request[B]): (Request[B], Option[RequestInterceptor[C]])
}

package utopia.disciple.controller

import utopia.disciple.http.request.Request

import scala.language.implicitConversions

object RequestInterceptor
{
	// IMPLICIT    ---------------------
	
	/**
	  * @param f An interceptor function to wrap.
	  *          Receives an outgoing request, yields a modified copy to send out.
	  * @return A new request interceptor based on the specified function
	  */
	implicit def apply(f: Request => Request): RequestInterceptor = new InterceptorFunctionWrapper(f)
	
	/**
	  * Wraps a function into a non-modifying request interceptor
	  * @param f A function which receives all outgoing requests
	  * @tparam U Arbitrary function result type
	  * @return A new request interceptor based on the specified function
	  */
	def readOnly[U](f: Request => U) = apply { r => f(r); r }
	
	
	// NESTED   ------------------------
	
	private class InterceptorFunctionWrapper(f: Request => Request) extends RequestInterceptor
	{
		override def intercept(request: Request): Request = f(request)
	}
}

/**
  * Request interceptors gain access to all request before they are forwarded to the server.
  * These interceptors are given the possibility to modify requests before they are sent out.
  * @author Mikko Hilpinen
  * @since 19.1.2023, v1.5.4
  */
trait RequestInterceptor
{
	/**
	  * Allows this interceptor to gain access to, and possibly modify an outgoing request
	  * @param request A request that is about to be forwarded to the server
	  * @return A potentially modified copy of the request, that will be sent instead
	  */
	def intercept(request: Request): Request
}

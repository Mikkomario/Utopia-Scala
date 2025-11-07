package utopia.nexus.controller.api.interceptor

import utopia.nexus.model.request.Request

import scala.language.implicitConversions

object RequestInterceptorFactory
{
	// IMPLICIT ----------------------
	
	/**
	 * @param newInterceptor A function for constructing a new request interceptor
	 * @tparam C Type of the used request context
	 * @return A new request interceptor factory
	 */
	implicit def apply[C](newInterceptor: => RequestInterceptor[C]): RequestInterceptorFactory[C] =
		apply { _ => newInterceptor }
	/**
	 * @param f A function for constructing a new request interceptor, based on a request
	 * @tparam C Type of the used request context
	 * @return A new request interceptor factory
	 */
	implicit def apply[C](f: Request[Any] => RequestInterceptor[C]): RequestInterceptorFactory[C] =
		new _RequestInterceptorFactory[C](f)
	
	
	// NESTED   ----------------------
	
	private class _RequestInterceptorFactory[C](f: Request[Any] => RequestInterceptor[C])
		extends RequestInterceptorFactory[C]
	{
		override def apply(request: Request[Any]): RequestInterceptor[C] = f(request)
	}
}

/**
 * An interface for constructing new request interceptors
 * @tparam C Type of the applicable request context
 * @author Mikko Hilpinen
 * @since 07.11.2025, v2.0
 */
trait RequestInterceptorFactory[C] extends InterceptRequest[C]
{
	// ABSTRACT -------------------------
	
	/**
	 * @param request The request the generated interceptor will interact with
	 * @return A new request interceptor
	 */
	def apply(request: Request[Any]): RequestInterceptor[C]
	
	
	// IMPLEMENTED  --------------------
	
	override def intercept[B](request: Request[B]): (Request[B], Option[RequestInterceptor[C]]) =
		request -> Some(apply(request))
}

package utopia.nexus.interceptor

import utopia.flow.util.Mutate
import utopia.nexus.http.Request

import scala.language.implicitConversions

object RequestInterceptor
{
	// IMPLICIT ------------------------
	
	implicit def apply(f: Mutate[Request]): RequestInterceptor = new _RequestInterceptor(f)
	
	implicit def readOnly(f: Request => Unit): RequestInterceptor = new _RequestInterceptor({ r => f(r); r })
	
	
	// NESTED   ------------------------
	
	private class _RequestInterceptor(f: Mutate[Request]) extends RequestInterceptor
	{
		override def intercept(request: Request): Request = f(request)
	}
}

/**
  * Common trait for interfaces that intercept requests, possibly modifying them
  * @author Mikko Hilpinen
  * @since 16.09.2024, v1.9.4
  */
trait RequestInterceptor
{
	/**
	  * @param request Request to intercept
	  * @return Possibly modified request
	  */
	def intercept(request: Request): Request
}

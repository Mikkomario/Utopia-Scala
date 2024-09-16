package utopia.nexus.interceptor

import utopia.nexus.http.{Request, Response}

import scala.language.implicitConversions

object ResponseInterceptor
{
	// IMPLICIT    --------------------
	
	implicit def apply(f: (Response, Request) => Response): ResponseInterceptor = new _ResponseInterceptor(f)
	
	implicit def readOnly(f: (Response, Request) => Unit): ResponseInterceptor =
		new _ResponseInterceptor({ (res, req) => f(res, req); res })
	
	
	// NESTED   -----------------------
	
	private class _ResponseInterceptor(f: (Response, Request) => Response) extends ResponseInterceptor
	{
		override def intercept(response: Response, request: Request): Response = f(response, request)
	}
}

/**
  * Common trait for interfaces that read and possibly modify outgoing server responses
  * @author Mikko Hilpinen
  * @since 16.09.2024, v1.9.4
  */
trait ResponseInterceptor
{
	/**
	  * @param response Outgoing response
	  * @param request Request that originated this response
	  * @return Possibly modified version of the specified response
	  */
	def intercept(response: Response, request: Request): Response
}

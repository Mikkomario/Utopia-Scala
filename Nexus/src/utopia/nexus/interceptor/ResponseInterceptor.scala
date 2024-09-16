package utopia.nexus.interceptor

import utopia.flow.util.Mutate
import utopia.nexus.http.Response

import scala.language.implicitConversions

object ResponseInterceptor
{
	// IMPLICIT    --------------------
	
	implicit def apply(f: Mutate[Response]): ResponseInterceptor = new _ResponseInterceptor(f)
	
	implicit def readOnly(f: Response => Unit): ResponseInterceptor = new _ResponseInterceptor({ r => f(r); r })
	
	
	// NESTED   -----------------------
	
	private class _ResponseInterceptor(f: Mutate[Response]) extends ResponseInterceptor
	{
		override def intercept(response: Response): Response = f(response)
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
	  * @return Possibly modified version of the specified response
	  */
	def intercept(response: Response): Response
}

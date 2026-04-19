package utopia.disciple.controller

import org.apache.hc.client5.http.protocol.RedirectStrategy
import org.apache.hc.core5.http.protocol.HttpContext
import org.apache.hc.core5.http.{HttpRequest, HttpResponse}

import java.net.URI

/**
 * A redirect strategy that doesn't follow any redirects
 * @author Mikko Hilpinen
 * @since 19.04.2026, v1.9.3
 */
object NoRedirects extends RedirectStrategy
{
	override def isRedirected(httpRequest: HttpRequest, httpResponse: HttpResponse, httpContext: HttpContext) = false
	
	override def getLocationURI(httpRequest: HttpRequest, httpResponse: HttpResponse, httpContext: HttpContext): URI =
		null
}

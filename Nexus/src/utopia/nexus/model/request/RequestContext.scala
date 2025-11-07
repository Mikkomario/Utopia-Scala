package utopia.nexus.model.request

import utopia.access.model.{HasHeaders, Headers}

object RequestContext
{
	// OTHER    ---------------------
	
	/**
	 * @param request Request to wrap
	 * @tparam A Type of the request body value
	 * @return Request context wrapping the specified request
	 */
	def apply[A](request: Request[A]): RequestContext[A] = _RequestContext(request)
	
	
	// NESTED   ---------------------
	
	private case class _RequestContext[+A](request: Request[A]) extends RequestContext[A]
	{
		override def close(): Unit = ()
	}
}

/**
 * Contextual information, providing access to the handled request.
 * May contain a temporary state, which is closed once the request has resolved.
 * @author Mikko Hilpinen
 * @since 05.11.2025, v2.0
 */
trait RequestContext[+A] extends HasHeaders with AutoCloseable
{
	// ABSTRACT ---------------------
	
	/**
	 * @return The request being handled
	 */
	def request: Request[A]
	
	
	// IMPLEMENTED  -----------------
	
	override def headers: Headers = request.headers
}

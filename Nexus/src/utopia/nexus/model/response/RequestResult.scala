package utopia.nexus.model.response

import utopia.access.model.{Headered, Headers}
import utopia.access.model.enumeration.Status
import utopia.access.model.enumeration.Status.{Found, MovedPermanently, NoContent, OK}
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.MaybeEmpty
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.nexus.controller.write.WriteResponseBody
import utopia.nexus.model.response.RequestResult.{ContentResult, _ContentResult, _RequestResult}

import scala.language.implicitConversions

object RequestResult
{
	// IMPLICIT -------------------------
	
	/**
	 * Implicitly converts a status-description pair into a request result
	 * @param statusAndDescription A tuple containing both the response status, and a description to pass to the client
	 * @return A new request result based on the specified information
	 */
	implicit def described(statusAndDescription: (Status, String)): RequestResult =
		apply(statusAndDescription._1, statusAndDescription._2)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param content Content to include in the response (default = empty)
	 * @param status Status to send to the client (default = OK)
	 * @param headers Headers to include (default = empty)
	 * @return A new request result containing the specified information
	 */
	def apply(content: ResponseContent = ResponseContent.empty, status: Status = OK,
	          headers: Headers = Headers.empty): ContentResult =
	{
		if (content.isEmpty && headers.isEmpty)
			Empty(status)
		else
			_ContentResult(status, content, headers)
	}
	/**
	 * @param status Response status
	 * @param description A description to include
	 * @return A result consisting of the specified description & status
	 */
	def apply(status: Status, description: String): ContentResult =
		apply(ResponseContent.description(description), status)
	
	/**
	 * Creates a request result with a custom response body
	 * @param body Logic for writing the response body
	 * @param status Status to send to the client. Default = OK = 200.
	 * @param headers Headers to include. Default = empty.
	 * @return A new request result
	 */
	def withBody(body: WriteResponseBody, status: Status = OK, headers: Headers = Headers.empty): RequestResult =
		new _RequestResult(status, Left(body), headers)
	
	
	// NESTED   -------------------------
	
	/**
	 * Common trait for request results that specify their contents using a static [[ResponseContent]] instance.
	 */
	trait ContentResult extends RequestResult with MaybeEmpty[ContentResult] with View[Value] with EqualsBy
	{
		// ABSTRACT ---------------------
		
		/**
		 * @return The contents of this result
		 */
		def content: ResponseContent
		
		
		// COMPUTED ---------------------
		
		/**
		 * @return Description included with this result
		 */
		def description = content.description
		
		
		// IMPLEMENTED  -----------------
		
		override def self: ContentResult = this
		override def value: Value = content.value
		override def isEmpty: Boolean = content.isEmpty
		
		override def output: Either[WriteResponseBody, ResponseContent] = Right(content)
		override protected def equalsProperties: IterableOnce[Any] = Vector(content, status, headers)
		
		override def withStatus(newStatus: Status): ContentResult = _ContentResult(newStatus, content, headers)
		override def mapStatus(f: Mutate[Status]): ContentResult = withStatus(f(status))
		
		override def withHeaders(headers: Headers, overwrite: Boolean): RequestResult =
			_ContentResult(status, content, if (overwrite) headers else this.headers ++ headers)
		override def mapHeaders(f: Mutate[Headers]): RequestResult =
			_ContentResult(status, content, f(headers))
		
		
		// OTHER    ---------------------
		
		def withValue(newValue: Value) = mapContent { _.withValue(newValue) }
		def mapValue(f: Mutate[Value]) = mapContent { _.mapValue(f) }
		
		def withDescription(newDescription: String) = mapContent { _.withDescription(newDescription) }
		def mapDescription(f: Mutate[String]) = mapContent { _.mapDescription(f) }
		
		def mapContent(f: Mutate[ResponseContent]): ContentResult = withContent(f(content))
	}
	
	case object Empty extends Empty
	{
		// ATTRIBUTES   -----------------
		
		override val status: Status = NoContent
		
		
		// OTHER    --------------------
		
		/**
		 * @param status Status to send out to the client
		 * @return An empty result with the specified status
		 */
		def apply(status: Status): Empty = _Empty(status)
	}
	/**
	 * Common trait for empty [[RequestResult]] implementations
	 */
	trait Empty extends ContentResult
	{
		override val content = ResponseContent.empty
		override val headers: Headers = Headers.empty
	}
	
	/**
	 * A [[RequestResult]] to send out when the targeted contents have not been modified
	 * since the last request / the specified time threshold
	 */
	case object NotModified extends Empty
	{
		override val status: Status = Status.NotModified
	}
	
	/**
	 * A request result used for redirecting the client to another resource or to another site
	 * @param url Url to which the client is redirected to
	 * @param content The content to return with the response (default = empty)
	 * @param permanently Whether this redirect should be used in the future without accessing this resource
	 *                    first (default = false)
	 */
	case class Redirect(url: String, content: ResponseContent = ResponseContent.empty, permanently: Boolean = false)
		extends ContentResult
	{
		override val status: Status = if (permanently) MovedPermanently else Found
		override val headers = Headers.empty.withLocation(url)
	}
	
	private case class _ContentResult(status: Status, content: ResponseContent, headers: Headers) extends ContentResult
	
	private case class _Empty(status: Status) extends Empty
	
	private class _RequestResult(override val status: Status,
	                             override val output: Either[WriteResponseBody, ResponseContent],
	                             override val headers: Headers)
		extends RequestResult
}

/**
* Represents a result given for a request made to the API / server.
* @author Mikko Hilpinen
* @since 4.11.2025, v2.0 - Written based on the Result trait that was first introduced 24.5.2018
**/
trait RequestResult extends Headered[RequestResult]
{
    // ABSTRACT    ------------------------
    
    /**
     * Status of this result (as intended to be sent to the client)
     */
	def status: Status
	/**
	 * The contents of this result, specified as either:
	 *      - Right: Static content (a value and/or a possible description)
	 *      - Left: Custom (streamed) content
	 */
	def output: Either[WriteResponseBody, ResponseContent]
	
	
	// COMPUTED    ------------------------
	
	/**
	 * Whether this represents a successful request completion
	 */
	def isSuccess = status.isSuccess
	/**
	 * @return Whether this represents a failure response
	 */
	def isFailure = !isSuccess
	
	
	// IMPLEMENTED  ----------------------
	
	override def withHeaders(headers: Headers, overwrite: Boolean = false): RequestResult =
		new _RequestResult(status, output, if (overwrite) headers else this.headers ++ headers)
	
	
	// OTHER    --------------------------
	
	def withStatus(newStatus: Status): RequestResult = new _RequestResult(newStatus, output, headers)
	def mapStatus(f: Mutate[Status]): RequestResult = withStatus(f(status))
	
	def withContent(newContent: ResponseContent): ContentResult = _ContentResult(status, newContent, headers)
}
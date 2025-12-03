package utopia.nexus.controller.write

import utopia.access.model.Headers
import utopia.access.model.enumeration.Status
import utopia.nexus.http.Response
import utopia.nexus.model.response.ResponseContent

object ContentWriter
{
	// COMPUTED --------------------------
	
	/**
	 * @return Access to JSON-based content writer constructors
	 */
	def json = JsonContentWriter
	/**
	 * @return Access to XML-based content writer constructors
	 */
	def xml = XmlContentWriter
	/**
	 * @return Access to constructors for content writers that support both JSON and XML
	 */
	def jsonOrXml = JsonOrXmlContentWriter
}

/**
 * Common trait for interfaces that prepare [[ResponseContent]]s, so that they may be written into [[Response]] bodies
 * @tparam C Required contextual information
 * @author Mikko Hilpinen
 * @since 04.11.2025, v2.0
 */
trait ContentWriter[-C]
{
	// ABSTRACT ---------------------------
	
	/**
	 * Prepares response content, so that it may be written into the response body
	 * @param content Content to write the response body.
	 * @param status Status of the outgoing response
	 * @param headers Headers prepared for the outgoing response
	 * @param context Implicit contextual information
	 * @return Logic for writing the specified content into the response body,
	 *         plus the final status to yield
	 *         (some content writers place the status inside the content,
	 *         in which case the outer status may be different)
	 */
	def prepare(content: ResponseContent, status: Status, headers: Headers)
	           (implicit context: C): (WriteResponseBody, Status)
}

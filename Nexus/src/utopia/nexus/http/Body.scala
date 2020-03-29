package utopia.nexus.http

import utopia.access.http.Headers
import utopia.access.http.ContentType

/**
* This trait represents a request body or a part of that body (in case of multipart requests)
* @author Mikko Hilpinen
* @since 12.5.2018
**/
trait Body
{
    // ABSTRACT    -------------------
    
    /**
     * The headers for this body
     */
	def headers: Headers
	/**
	 * The content length for this body. May be undefined
	 */
	def contentLength: Option[Long]
	/**
	 * The content type for this body
	 */
	def contentType: ContentType
	/**
	 * The name of this part, if it has one
	 */
	def name: Option[String]
	
	
	// COMPUTED PROPERTIES    ---------
	
	/**
	 * Whether this body is empty
	 */
	def isEmpty = contentLength.contains(0) && !headers.isChunked
}
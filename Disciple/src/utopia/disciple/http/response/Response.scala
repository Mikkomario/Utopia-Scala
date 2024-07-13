package utopia.disciple.http.response

import utopia.access.http.{Headers, Status, StatusGroup}

/**
* Responses are sent by a server. Responses have a specific status and may contain a body
 * section. Context and content type determine, how the response body is parsed.
* @author Mikko Hilpinen
* @since 19.2.2018
**/
trait Response
{
	// ABSTRACT    ------------------
    
    /**
     * The HTTP status of this response
     */
    def status: Status
    
    /**
     * The HTTP headers of this response
     */
    def headers: Headers
    
    /*
     * The cookies associated with this response
     */
    // def cookies: Set[Cookie]
    
    
    // COMPUTED PROPERTIES    ---------------
    
    /**
     * @return Whether this response has a success (2XX) status
     */
    def isSuccess = status.group == StatusGroup.Success
    /**
      * @return Whether this response has a failure (4XX or 5XX) status
      */
    def isFailure = StatusGroup.failure.contains(status.group)
    
    /**
     * The content type of the response
     */
    def contentType = headers.contentType
    
    /**
     * The length of the response body
     */
    def contentLength = headers.contentLength
    
    /**
     * Whether the response is empty (no body)
     */
    def isEmpty = headers.isContentLengthProvided && contentLength == 0 && !headers.isChunked
}

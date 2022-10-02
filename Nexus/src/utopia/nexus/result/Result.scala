package utopia.nexus.result

import utopia.access.http.{Headers, Status, StatusGroup}
import utopia.access.http.Status._
import utopia.flow.generic.model.immutable.Value
import utopia.nexus.rest.Context

object Result
{
    /**
     * This result may be returned when there is no data to return
     */
    case object Empty extends Result
    {
        def status = NoContent
        def description = None
        def data = Value.empty
        def headers = Headers.empty
    }
    
    /**
     * This result should be returned for requests that have specified if modified since -header and content
	  * hasn't been modified
     */
    case object NotModified extends Result
    {
        def status = Status.NotModified
        def description = None
        def data = Value.empty
        def headers = Headers.empty
    }
    
	object Failure
	{
		/**
		 * Creates a new failure result with description
		 * @param status Failure status
		 * @param description Failure description
		 * @return A failure result
		 */
		def apply(status: Status, description: String) = new Failure(status, Some(description))
	}
	
    /**
     * This result may be returned when a request is invalid or when an error occurs
     */
    case class Failure(status: Status, description: Option[String] = None,
            data: Value = Value.empty, headers: Headers = Headers.empty) extends Result
    
    /**
     * This result may be returned when the API wants to return specific data
     */
    case class Success(data: Value = Value.empty, status: Status = OK,
            description: Option[String] = None, headers: Headers = Headers.empty) extends Result
	
	/**
	 * A result used for redirecting the client to another resource
	 * @param url Url to which the client is redirected to
	 * @param permanently Whether this redirect should be used in the future without accessing this resource
	 *                    first (default = false)
	 */
	case class Redirect(url: String, permanently: Boolean = false) extends Result
	{
		override def status = if (permanently) MovedPermanently else Found
		
		override def description = None
		
		override def data = Value.empty
		
		override def headers = Headers.empty.withLocation(url)
	}
}

/**
* API Responses are simple responses returned by a restful service. The responses can then be handled 
* and represented in different ways
* @author Mikko
* @since 24.5.2018
**/
trait Result
{
    // ABSTRACT    ------------------------
    
    /**
     * The status of the result
     */
	def status: Status
	
	/**
	 * The description for the result (optional)
	 */
	def description: Option[String]
	
	/**
	 * The data returned by this result
	 */
	def data: Value
	
	/**
	 * The header modifications for this result
	 */
	def headers: Headers
	
	
	// COMPUTED    ------------------------
	
	/**
	 * Whether this is a result of an successful operation
	 */
	def isSuccess = status.group == StatusGroup.Success
	
	
	// OTHER METHODS    ------------------
	
	/**
	 * Converts this result into a response for the specified request
	 */
	def toResponse(implicit context: Context) = context.resultParser(this, context.request)
}


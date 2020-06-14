package utopia.annex.model

import utopia.access.http.StatusGroup.ServerError
import utopia.access.http.{Status, StatusGroup}
import utopia.disciple.http.response.BufferedResponse
import utopia.flow.datastructure.immutable.Value

/**
  * Represents a result of a sent request
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
sealed trait RequestResult
{
	/**
	  * @return Whether result should be considered a success
	  */
	def isSuccess: Boolean
	
	/**
	  * @return Whether this result should be considered a failure
	  */
	def isFailure = !isSuccess
}

/**
  * This request result is used when no response is received from the server
  * @param error An error associated with this failure
  */
case class NoConnection(error: Throwable) extends RequestResult
{
	override def isSuccess = false
}

/**
  * A common trait for both success and failure responses
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
sealed trait Response extends RequestResult
{
	/**
	  * @return Response status
	  */
	def status: Status
}

object Response
{
	// OTHER    -------------------------------
	
	/**
	  * Converts a buffered response to a response model
	  * @param response A buffered response received through Gateway
	  * @return Either success or failure response based on response status
	  */
	def from(response: BufferedResponse[Value]): Response =
	{
		if (StatusGroup.failure.contains(response.status.group))
			Failure(response.status, response.body.string)
		else
			Success(response.status, ResponseBody(response.body))
	}
	
	
	// NESTED   -------------------------------
	
	/**
	  * Success responses are used when the server successfully appropriates the request
	  * @param status Status returned by the server
	  * @param body Response body
	  */
	case class Success(status: Status, body: ResponseBody) extends Response
	{
		override def isSuccess = true
	}
	
	/**
	  * Failure responses are used when the server refuses to appropriate the request
	  * @param status Status returned by the server
	  * @param message Error description or other message within the response body (optional)
	  */
	case class Failure(status: Status, message: Option[String] = None) extends Response
	{
		// COMPUTED --------------------------
		
		/**
		  * @return Whether this failure should be considered client-originated
		  */
		def isCausedByClient = status.group != ServerError
		
		
		// IMPLEMENTED  ----------------------
		
		override def isSuccess = false
	}
}
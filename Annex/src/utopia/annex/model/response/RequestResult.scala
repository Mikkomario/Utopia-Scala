package utopia.annex.model.response

import utopia.access.http.Status.OK
import utopia.access.http.StatusGroup.ServerError
import utopia.access.http.{Headers, Status}
import utopia.disciple.model.error.RequestFailedException
import utopia.flow.util.NotEmpty
import utopia.flow.util.StringExtensions._

import scala.util.{Failure, Try}

/**
  * Represents a result of a sent request
  * @tparam A Type of the expected successful response value / body contents
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
sealed trait RequestResult2[+A]
{
	/**
	  * @return Whether result should be considered a success
	  */
	def isSuccess: Boolean
	/**
	  * @return Whether this result should be considered a failure
	  */
	def isFailure = !isSuccess
	
	/**
	  * @return Parsed response body if successful.
	  *         Failure if this is not a successful response.
	  */
	def toTry: Try[A]
	
	/**
	  * Maps the contents of this result, if successful
	  * @param f A mapping function applied to response contents
	  * @tparam B Type of the mapping results
	  * @return Copy of this response with mapped contents.
	  *         If this response is a failure. Returns this response.
	  */
	def map[B](f: A => B): RequestResult2[B]
}

sealed trait RequestFailure2 extends RequestResult2[Nothing]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Cause for this request failure
	  */
	def cause: Throwable
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return A failure based on this result
	  */
	def toFailure[A] = Failure[A](cause)
	
	
	// IMPLEMENTED  ----------------------
	
	override def isSuccess = false
	
	override def toTry = toFailure
	
	override def map[B](f: Nothing => B) = this
}

/**
  * A status used when a request is not sent for some reason
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
sealed trait RequestNotSent2 extends RequestFailure2
{
	/**
	  * @return A throwable error based on this state
	  */
	@deprecated("Please use .cause instead", "v1.6")
	def toException: Throwable = cause
}

object RequestNotSent2
{
	/**
	  * Status generated when request gets deprecated before it is successfully sent
	  */
	case object RequestWasDeprecated2 extends RequestNotSent2
	{
		override def cause = new RequestFailedException("Request was deprecated")
		
		override def toString = "Request deprecation"
	}
	
	/**
	  * Status used when request can't be sent due to some error in the request or the request system
	  * @param cause Associated error
	  */
	case class RequestSendingFailed2(cause: Throwable) extends RequestNotSent2
	{
		@deprecated("Please use .cause instead", "v1.6")
		def error = cause
		
		override def toString = s"Request sending failed (${cause.getMessage})"
	}
}

/**
  * A common trait for both success and failure responses
  * @tparam A Response content / parsed body type expected in successful responses
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
sealed trait Response2[+A] extends RequestResult2[A] with utopia.disciple.http.response.Response
{
	// IMPLEMENTED  ---------------------------
	
	override def isFailure = !isSuccess
	
	override def map[B](f: A => B): Response2[B]
	
	
	// OTHER    -------------------------------
	
	/**
	  * If this is a successful response, applies the specified mapping function.
	  * If mapping fails (i.e. yields Left), converts this response into a failure response.
	  * @param f Mapping function to apply.
	  *          On success, yields Right with the mapped value.
	  *          On failure, yields Left with the status and error message to assign.
	  * @tparam B Type of successful map result
	  * @return Mapped copy of this response
	  */
	def tryMap[B](f: A => Either[(Status, String), B]): Response2[B]
}

object Response2
{
	// NESTED   -------------------------------
	
	/**
	  * Success responses are used when the server successfully appropriates the request
	  * @param value Parsed response value (typically from the response body)
	  * @param status Status returned by the server
	  * @param headers Response headers
	  */
	case class Success[+A](value: A, status: Status = OK, headers: Headers = Headers.empty) extends Response2[A]
	{
		// IMPLEMENTED  ---------------------
		
		override def isSuccess = true
		
		override def toTry = scala.util.Success(value)
		override def toString = s"$status: $value"
		
		override def map[B](f: A => B) = copy(value = f(value))
		override def tryMap[B](f: A => Either[(Status, String), B]): Response2[B] = f(value) match {
			case Right(newValue) => copy(value = newValue)
			case Left((status, message)) => Failure(status, message, headers)
		}
	}
	
	/**
	  * Failure responses are used when the server refuses to appropriate the request.
	  * Typically this is the case when the server responds with 4XX or 5XX status,
	  * but may also be caused by a critical parse failure.
	  * @param status Status returned by the server
	  * @param message Error description or other message within the response body. May be empty.
	  * @param headers Headers sent along with this response
	  */
	case class Failure(status: Status, message: String = "", headers: Headers = Headers.empty)
		extends Response2[Nothing] with RequestFailure2
	{
		// COMPUTED --------------------------
		
		/**
		  * @return Whether this failure should be considered client-originated
		  */
		def isCausedByClient = status.group != ServerError
		
		/**
		  * @return An exception based on this failure
		  */
		def toException = {
			val errorMessage = NotEmpty(message) match {
				case Some(message) => s"$message ($status)"
				case None => s"Server responded with status $status"
			}
			new RequestFailedException(errorMessage)
		}
		
		
		// IMPLEMENTED  ----------------------
		
		override def isSuccess = false
		override def isFailure = true
		
		override def cause = toException
		
		override def toString = s"$status${message.mapIfNotEmpty { message => s": $message" }}"
		
		override def map[B](f: Nothing => B) = this
		override def tryMap[B](f: Nothing => Either[(Status, String), B]) = this
	}
}
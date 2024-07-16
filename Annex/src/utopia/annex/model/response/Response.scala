package utopia.annex.model.response

import utopia.access.http.StatusGroup.ServerError
import utopia.access.http.{Headers, Status, StatusGroup}
import utopia.disciple.http.response.BufferedResponse
import utopia.disciple.model.error.RequestFailedException
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.NotEmpty
import utopia.flow.util.StringExtensions._

import scala.util.{Failure, Try}

/**
  * Represents a result of a sent request
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
@deprecated("Will be replaced with a new version", "v1.8")
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
	
	/**
	  * @return Either an empty success or a failure, based on this response's status
	  */
	def toEmptyTry: Try[Unit]
	
	/**
	  * If this is a successful response, attempts to parse its contents into a single entity
	  * @param parser Parser used to interpret response body
	  * @tparam A Type of parse result
	  * @return Parsed response content on success. Failure if this response was not a success,
	  *         if response body was empty or if parsing failed.
	  */
	def singleParsedFromSuccess[A](parser: FromModelFactory[A]): Try[A]
	/**
	  * If this is a successful response, attempts to parse its contents into a vector of entities
	  * @param parser Parser used to interpret response body elements
	  * @tparam A Type of parse result
	  * @return Parsed response content on success. Failure if this response was not a success or if parsing failed.
	  */
	def manyParsedFromSuccess[A](parser: FromModelFactory[A]): Try[Seq[A]]
}

@deprecated("Will be replaced with a new version", "v1.8")
sealed trait RequestFailure extends RequestResult
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
	
	override def toEmptyTry = toFailure
	override def singleParsedFromSuccess[A](parser: FromModelFactory[A]) = toFailure
	override def manyParsedFromSuccess[A](parser: FromModelFactory[A]) = toFailure
}

/**
  * A status used when a request is not sent for some reason
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
@deprecated("Will be replaced with a new version", "v1.8")
sealed trait RequestNotSent extends RequestFailure
{
	/**
	  * @return A throwable error based on this state
	  */
	@deprecated("Please use .cause instead", "v1.6")
	def toException: Throwable = cause
}

object RequestNotSent
{
	/**
	  * Status generated when request gets deprecated before it is successfully sent
	  */
	@deprecated("Will be replaced with a new version", "v1.8")
	case object RequestWasDeprecated extends RequestNotSent
	{
		override def cause = new RequestFailedException("Request was deprecated")
		
		override def toString = "Request deprecation"
	}
	
	/**
	  * Status used when request can't be sent due to some error in the request or the request system
	  * @param cause Associated error
	  */
	@deprecated("Will be replaced with a new version", "v1.8")
	case class RequestSendingFailed(cause: Throwable) extends RequestNotSent
	{
		@deprecated("Please use .cause instead", "v1.6")
		def error = cause
		
		override def toString = s"Request sending failed (${cause.getMessage})"
	}
}

/**
  * A common trait for both success and failure responses
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
@deprecated("Will be replaced with a new version", "v1.8")
sealed trait Response extends RequestResult
{
	// ABSTRACT -------------------------------
	
	/**
	  * @return Response status
	  */
	def status: Status
	/**
	  * @return Response headers
	  */
	def headers: Headers
}

@deprecated("Will be replaced with a new version", "v1.8")
object Response
{
	// OTHER    -------------------------------
	
	/**
	  * Converts a buffered response to a response model
	  * @param response A buffered response received through Gateway
	  * @return Either success or failure response based on response status
	  */
	def from(response: BufferedResponse[Value]): Response = {
		if (StatusGroup.failure.contains(response.status.group)) {
			// The error message may be embedded within a model, also
			val message = response.body("error", "description", "message").stringOr(response.body.getString)
			Failure(response.status, message, response.headers)
		}
		else
			Success(response.status, ResponseBody(response.body), response.headers)
	}
	
	
	// NESTED   -------------------------------
	
	/**
	  * Success responses are used when the server successfully appropriates the request
	  * @param status Status returned by the server
	  * @param body Response body
	  */
	@deprecated("Will be replaced with a new version", "v1.8")
	case class Success(status: Status, body: ResponseBody, headers: Headers) extends Response
	{
		override def isSuccess = true
		
		override def toString = s"$status: $body"
		
		override def toEmptyTry = scala.util.Success(())
		override def singleParsedFromSuccess[A](parser: FromModelFactory[A]) = body.tryParseSingleWith(parser)
		override def manyParsedFromSuccess[A](parser: FromModelFactory[A]) = body.vector(parser).parsed
	}
	
	/**
	  * Failure responses are used when the server refuses to appropriate the request
	  * @param status Status returned by the server
	  * @param message Error description or other message within the response body. May be empty.
	  * @param headers Headers sent along with the response
	  */
	@deprecated("Will be replaced with a new version", "v1.8")
	case class Failure(status: Status, message: String = "", headers: Headers)
		extends Response with RequestFailure
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
		
		override def cause = toException
		
		override def toString = s"$status${message.mapIfNotEmpty { message => s": $message" }}"
	}
}
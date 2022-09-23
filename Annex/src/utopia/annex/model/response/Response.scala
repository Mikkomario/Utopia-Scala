package utopia.annex.model.response

import utopia.access.http.StatusGroup.ServerError
import utopia.access.http.{Headers, Status, StatusGroup}
import utopia.disciple.model.error.RequestFailedException
import utopia.disciple.http.response.BufferedResponse
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.FromModelFactory

import scala.util.Try

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
	// ABSTRACT -------------------------------
	
	/**
	  * @return Response status
	  */
	def status: Status
	/**
	  * @return Response headers
	  */
	def headers: Headers
	
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
	def manyParsedFromSuccess[A](parser: FromModelFactory[A]): Try[Vector[A]]
}

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
			val message = response.body("error", "description", "message").string.orElse(response.body.string)
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
	case class Success(status: Status, body: ResponseBody, headers: Headers) extends Response
	{
		override def isSuccess = true
		
		override def toEmptyTry = scala.util.Success(())
		
		override def singleParsedFromSuccess[A](parser: FromModelFactory[A]) = body.tryParseSingleWith(parser)
		
		override def manyParsedFromSuccess[A](parser: FromModelFactory[A]) = body.vector(parser).parsed
	}
	
	/**
	  * Failure responses are used when the server refuses to appropriate the request
	  * @param status Status returned by the server
	  * @param message Error description or other message within the response body (optional)
	  */
	case class Failure(status: Status, message: Option[String] = None, headers: Headers) extends Response
	{
		// COMPUTED --------------------------
		
		/**
		  * @return Whether this failure should be considered client-originated
		  */
		def isCausedByClient = status.group != ServerError
		
		/**
		  * @return An exception based on this failure
		  */
		def toException =
		{
			val errorMessage = message match
			{
				case Some(message) => s"$message ($status)"
				case None => s"Server responded with status $status"
			}
			new RequestFailedException(errorMessage)
		}
		
		/**
		  * @return A scala.util failure based on this failure state
		  */
		def toFailure[A] = scala.util.Failure[A](toException)
		
		
		// IMPLEMENTED  ----------------------
		
		override def isSuccess = false
		
		override def toEmptyTry = toFailure
		
		override def singleParsedFromSuccess[A](parser: FromModelFactory[A]) = toFailure
		
		override def manyParsedFromSuccess[A](parser: FromModelFactory[A]) = toFailure
	}
}
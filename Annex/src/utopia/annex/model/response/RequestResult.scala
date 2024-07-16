package utopia.annex.model.response

import utopia.access.http.Status.{InternalServerError, OK}
import utopia.access.http.StatusGroup.ServerError
import utopia.access.http.{Headers, Status}
import utopia.disciple.model.error.RequestFailedException
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
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
	// ABSTRACT --------------------------
	
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
	/**
	  * If this is a successful response, applies the specified mapping function.
	  * If mapping fails (i.e. yields Left), converts this response into a failure response instead.
	  * @param f Mapping function to apply.
	  *             - On success, yields Right with the mapped value.
	  *             - On failure, yields Left with the status and error message to assign.
	  * @tparam B Type of successful map result
	  * @return Mapped copy of this response
	  */
	def tryMap[B](f: A => Either[(Status, String), B]): RequestResult2[B]
	
	
	// COMPUTED --------------------------
	
	@deprecated("Deprecated for removal. Please use .toTry instead", "v1.8")
	def toEmptyTry = toTry.map { _ => () }
	
	
	// OTHER    --------------------------
	
	/**
	  * If this is a successful response, applies the specified mapping function.
	  * If mapping fails, converts this response into a failure response instead.
	  * @param parseFailureStatus Status assigned to a failure response in case 'f' yields a failure.
	  * @param f Mapping function to apply. May yield a failure.
	  * @tparam B Type of successful map result
	  * @return Mapped copy of this response
	  */
	def tryMap[B](parseFailureStatus: => Status)(f: A => Try[B]): RequestResult2[B] = tryMap { value =>
		f(value) match {
			case scala.util.Success(parsed) => Right(parsed)
			case scala.util.Failure(error) => Left(parseFailureStatus -> error.getMessage)
		}
	}
}

object RequestResult2
{
	// Adds additional functions for RequestResults of type Value, which is the only data type supported before v1.8
	implicit class RequestValueResult(val r: RequestResult2[Value]) extends AnyVal
	{
		/**
		  * Applies a from-model-parser to this result, transforming response contents, if this is a success.
		  * Assumes that successful responses contain a single [[utopia.flow.generic.model.immutable.Model]] value.
		  * Parsing failures will be converted into failure responses.
		  * @param parser A parser to use for transforming the response contents
		  * @param parseFailureStatus Status assigned for this response in case parsing fails.
		  *                           Default = 500 = Internal Server Error
		  * @tparam A Type of successful parse results
		  * @return Copy of this result with the specified parser / processing applied to it
		  */
		def parsingOneWith[A](parser: FromModelFactory[A], parseFailureStatus: => Status = InternalServerError) =
			r.tryMap(parseFailureStatus) { _.tryModel.flatMap(parser.apply) }
		/**
		  * Applies a from-model-parser to this result, transforming response contents, if this is a success.
		  * Assumes that successful responses contain 0-n [[utopia.flow.generic.model.immutable.Model]] values
		  * as a vector / array.
		  * Parsing failures will be converted into failure responses.
		  * @param parser A parser to use for transforming the response contents
		  * @param parseFailureStatus Status assigned for this response in case parsing fails.
		  *                           Default = 500 = Internal Server Error
		  * @tparam A Type of successful parse results
		  * @return Copy of this result with the specified parser / processing applied to it
		  */
		def parsingManyWith[A](parser: FromModelFactory[A], parseFailureStatus: => Status = InternalServerError) =
			r.tryMap(parseFailureStatus) { _.tryVectorWith { _.tryModel.flatMap(parser.apply) } }
		/**
		  * Applies a from-model-parser to this result, transforming response contents, if this is a non-empty success.
		  * Assumes that successful responses contain a single [[utopia.flow.generic.model.immutable.Model]] value
		  * or are empty.
		  * Parsing failures will be converted into failure responses.
		  * @param parser A parser to use for transforming the response contents
		  * @param parseFailureStatus Status assigned for this response in case parsing fails.
		  *                           Default = 500 = Internal Server Error
		  * @tparam A Type of successful parse results
		  * @return Copy of this result with the specified parser / processing applied to it.
		  *         Will contain None in case this is a successful empty response.
		  */
		def parsingOptionWith[A](parser: FromModelFactory[A], parseFailureStatus: => Status = InternalServerError) =
			r.tryMap[Option[A]](parseFailureStatus) { value =>
				if (value.isEmpty)
					scala.util.Success(None)
				else
					value.tryModel.flatMap(parser.apply).map { Some(_) }
			}
		
		/**
		  * If this is a successful response, attempts to parse its contents into a single entity
		  * @param parser Parser used to interpret the response body value
		  * @tparam A Type of parse result
		  * @return Parsed response content on success.
		  *         Failure if this response was not a success, or if the parsing failed.
		  */
		def tryParseOne[A](parser: FromModelFactory[A]): Try[A] = r.toTry.flatMap { _.tryModel.flatMap(parser.apply) }
		/**
		  * If this is a successful response, attempts to parse its contents into a vector of entities
		  * @param parser Parser used to interpret response body elements
		  * @tparam A Type of parse result
		  * @return Parsed response content on success. Failure if this response was not a success or if parsing failed.
		  */
		def tryParseMany[A](parser: FromModelFactory[A]): Try[Seq[A]] =
			r.toTry.flatMap { _.tryVectorWith { _.tryModel.flatMap(parser.apply) } }
		/**
		  * If this is a non-empty successful response, attempts to parse its contents into a single entity
		  * @param parser Parser used to interpret the response body value
		  * @tparam A Type of parse result
		  * @return Parsed response content on success.
		  *         Failure if this response was not a success, or if the parsing failed.
		  *         If this response was empty (and successful), yields None.
		  */
		def tryParseOption[A](parser: FromModelFactory[A]) =
			r.toTry.flatMap { value =>
				if (value.isEmpty)
					scala.util.Success(None)
				else
					value.tryModel.flatMap(parser.apply).map { Some(_) }
			}
		
		// These two functions are added for backwards-compatibility
		/**
		  * If this is a successful response, attempts to parse its contents into a single entity
		  * @param parser Parser used to interpret response body
		  * @tparam A Type of parse result
		  * @return Parsed response content on success. Failure if this response was not a success,
		  *         if response body was empty or if parsing failed.
		  */
		@deprecated("Renamed to .tryParseOne", "v1.8")
		def singleParsedFromSuccess[A](parser: FromModelFactory[A]): Try[A] = tryParseOne(parser)
		/**
		  * If this is a successful response, attempts to parse its contents into a vector of entities
		  * @param parser Parser used to interpret response body elements
		  * @tparam A Type of parse result
		  * @return Parsed response content on success. Failure if this response was not a success or if parsing failed.
		  */
		@deprecated("Renamed to .tryParseMany", "v1.8")
		def manyParsedFromSuccess[A](parser: FromModelFactory[A]): Try[Seq[A]] = tryParseMany(parser)
	}
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
	override def tryMap[B](f: Nothing => Either[(Status, String), B]) = this
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
	override def tryMap[B](f: A => Either[(Status, String), B]): Response2[B]
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
	
	
	// EXTENSIONS   -------------------------
	
	// Adds implicit backwards-compatibility to the previous RequestResult / Response.Success version
	// which was always of type Value and used the ResponseBody interface
	implicit class ValueSuccess(val s: Success[Value]) extends AnyVal
	{
		@deprecated("Deprecated for removal. Please use .value or apply additional parsers, etc.", "v1.8")
		def body = ResponseBody(s.value)
	}
}
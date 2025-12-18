package utopia.annex.model.response

import utopia.access.model.Headers
import utopia.access.model.enumeration.Status
import utopia.access.model.enumeration.Status.{InternalServerError, OK}
import utopia.access.model.enumeration.StatusGroup.ServerError
import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.disciple.model.error.RequestFailedException
import utopia.disciple.model.response
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.{Mutate, NotEmpty}
import utopia.flow.util.result.MayHaveFailed.FailureLike
import utopia.flow.util.StringExtensions._
import utopia.flow.util.result.{MayHaveFailed, MayHaveFailedLike, TryCatch}

import scala.util.{Failure, Success, Try}

/**
  * Represents a result of an outgoing request
  * @tparam A Type of the expected successful response value / body contents
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
sealed trait RequestResult[+A]
	extends MayHaveFailed[A] with MayHaveFailedLike[A, RequestResult, RequestResult, TryCatch]
{
	// ABSTRACT --------------------------
	
	/**
	  * If this is a successful response, applies the specified mapping function.
	  * If mapping fails (i.e. yields Left), converts this response into a failure response instead.
	  * @param f Mapping function to apply.
	  *             - On success, yields Right with the mapped value.
	  *             - On failure, yields Left with the status and error message to assign.
	  * @tparam B Type of successful map result
	  * @return Mapped copy of this response
	  */
	def flatMap[B](f: A => Either[String, B]): RequestResult[B]
	/**
	 * If this is a successful response, applies the specified mapping function.
	 * If mapping fails, converts this response into a failure response instead.
	 * @param parseFailureStatus Status assigned to a failure response in case 'f' yields a failure.
	 * @param f Mapping function to apply. May yield a failure.
	 * @tparam B Type of successful map result
	 * @return Mapped copy of this response
	 */
	def tryMap[B](parseFailureStatus: => Status)(f: A => Try[B]): RequestResult[B]
	
	
	// COMPUTED --------------------------
	
	@deprecated("Deprecated for removal. Please use .toTry instead", "v1.8")
	def toEmptyTry = toTry.map { _ => () }
	
	
	// IMPLEMENTED  ----------------------
	
	override def isFailure = !isSuccess
}

object RequestResult
{
	// OTHER    --------------------------
	
	/**
	 * @param result A result of some kind
	 * @tparam A Type of the wrapped value on success
	 * @return A request result based on the specified value
	 */
	def from[A](result: MayHaveFailed[A]) = result match {
		case result: RequestResult[A] => result
		case failure: MayHaveFailed.Failure => RequestSendingFailed(failure.cause)
		case result =>
			result.toTry match {
				case Success(value) => Response.Success(value)
				case Failure(error) => RequestSendingFailed(error)
			}
	}
	
	
	// EXTENSIONS   ----------------------
	
	// Adds additional functions for RequestResults of type Value, which is the only data type supported before v1.8
	implicit class RequestValueResult(val r: RequestResult[Value]) extends AnyVal
	{
		/**
		  * Applies a from-model-parser to this result, transforming response contents, if this is a success.
		  * Assumes that successful responses contain a single [[utopia.flow.generic.model.immutable.Model]] value.
		  * Parsing failures will be converted into failure responses.
		  * @param parser A parser to use for transforming the response contents
		  * @tparam A Type of successful parse results
		  * @return Copy of this result with the specified parser / processing applied to it
		  */
		def parseOne[A](parser: FromModelFactory[A]) = r.tryMap { _.tryModel.flatMap(parser.apply) }
		@deprecated("Renamed to .parseOne(FromModelFactory)", "v1.12")
		def parsingOneWith[A](parser: FromModelFactory[A]) = parseOne(parser)
		/**
		  * Applies a from-model-parser to this result, transforming response contents, if this is a success.
		  * Assumes that successful responses contain 0-n [[utopia.flow.generic.model.immutable.Model]] values
		  * as a vector / array.
		  * Parsing failures will be converted into failure responses.
		  * @param parser A parser to use for transforming the response contents
		  * @tparam A Type of successful parse results
		  * @return Copy of this result with the specified parser / processing applied to it
		  */
		def parseMany[A](parser: FromModelFactory[A]) =
			r.tryMap { _.tryVectorWith { _.tryModel.flatMap(parser.apply) } }
		@deprecated("Renamed to .parseMany(FromModelFactory)", "v1.12")
		def parsingManyWith[A](parser: FromModelFactory[A]) = parseMany(parser)
		/**
		  * Applies a from-model-parser to this result, transforming response contents, if this is a non-empty success.
		  * Assumes that successful responses contain a single [[utopia.flow.generic.model.immutable.Model]] value
		  * or are empty.
		  * Parsing failures will be converted into failure responses.
		  * @param parser A parser to use for transforming the response contents
		  * @tparam A Type of successful parse results
		  * @return Copy of this result with the specified parser / processing applied to it.
		  *         Will contain None in case this is a successful empty response.
		  */
		def parseOption[A](parser: FromModelFactory[A]) =
			r.tryMap[Option[A]] { value: Value =>
				if (value.isEmpty)
					scala.util.Success(None)
				else
					value.tryModel.flatMap(parser.apply).map { Some(_) }
			}
		@deprecated("Renamed to .parseOption(FromModelFactory)", "v1.12")
		def parsingOptionWith[A](parser: FromModelFactory[A]) = parseOption(parser)
		
		/**
		  * If this is a successful response, attempts to parse its contents into a single entity
		  * @param parser Parser used to interpret the response body value
		  * @tparam A Type of parse result
		  * @return Parsed response content on success.
		  *         Failure if this response was not a success, or if the parsing failed.
		  */
		@deprecated("Please use .parseOne(FromModelFactory).toTry instead", "v1.12")
		def tryParseOne[A](parser: FromModelFactory[A]): Try[A] = r.toTry.flatMap { _.tryModel.flatMap(parser.apply) }
		/**
		  * If this is a successful response, attempts to parse its contents into a vector of entities
		  * @param parser Parser used to interpret response body elements
		  * @tparam A Type of parse result
		  * @return Parsed response content on success. Failure if this response was not a success or if parsing failed.
		  */
		@deprecated("Please use .parseMany(FromModelFactory).toTry instead", "v1.12")
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
		@deprecated("Please use .parseOption(FromModelFactory).toTry instead", "v1.12")
		def tryParseOption[A](parser: FromModelFactory[A]) = parseOption(parser).toTry
		
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

sealed trait RequestFailure extends FailureLike[RequestFailure] with MayHaveFailed.Failure with RequestResult[Nothing]
{
	// COMPUTED ----------------------
	
	/**
	  * @return A failure based on this result
	  */
	def toFailure[A] = Failure[A](cause)
	
	
	// IMPLEMENTED  ----------------------
	
	override def self: RequestFailure = this
	
	override def map[B](f: Nothing => B) = this
	override def flatMap[B](f: Nothing => Either[String, B]) = this
	override def tryMap[B](f: Nothing => Try[B]) = this
	override def tryMapCatching[B](f: Nothing => TryCatch[B]): TryCatch[B] = toTryCatch
	override def tryMap[B](parseFailureStatus: => Status)(f: Nothing => Try[B]) = this
	
	override def catching[B >: Nothing](partialFailures: => IterableOnce[Throwable]): TryCatch[B] =
		TryCatch.Failure(cause)
}

/**
  * A status used when a request is not sent for some reason
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
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
	case object RequestWasDeprecated extends RequestNotSent
	{
		override def cause = new RequestFailedException("Request was deprecated")
		
		override def toString = "Request deprecation"
	}
	
	/**
	  * Status used when request can't be sent due to some error in the request or the request system
	  * @param cause Associated error
	  */
	case class RequestSendingFailed(cause: Throwable) extends RequestNotSent
	{
		@deprecated("Please use .cause instead", "v1.6")
		def error = cause
		
		override def toString = s"Request sending failed (${cause.getMessage})"
	}
}

/**
 * Common trait for copyable response representation implementations
 * @tparam A Type of the expected successful response value / body contents
 * @tparam Repr (Implementing) type of this response
 * @tparam R Type of mapping result wrappers, which may represent either a success or a failure
 */
sealed trait ResponseLike[+A, +Repr, +R[X] <: RequestResult[X]]
	extends RequestResult[A] with response.Response with MayHaveFailedLike[A, R, Response, TryCatch]
{
	// ABSTRACT -------------------------------
	
	/**
	 * @param newValue New success value to assign to this response
	 * @tparam B Type of the new success value
	 * @return A copy of this response as a success, containing the specified value
	 */
	def toSuccessWithValue[B](newValue: B): Response.Success[B]
	
	/**
	 * @param status New status to assign to this response
	 * @return A copy of this response with the specified status.
	 *         Note: This response reserves its original success/failure state, regardless of the new status
	 */
	def withStatus(status: Status): Repr
	/**
	 * @param headers New headers to assign to this response
	 * @return A copy of this response with the specified headers
	 */
	def withHeaders(headers: Headers): Repr
	
	override def flatMap[B](f: A => Either[String, B]): R[B]
	
	
	// IMPLEMENTED  ---------------------------
	
	override def isFailure = !isSuccess
	
	override def tryMap[B](parseFailureStatus: => Status)(f: A => Try[B]): R[B] = flatMap { value =>
		f(value) match {
			case scala.util.Success(parsed) => Right(parsed)
			case scala.util.Failure(error) => Left(error.getMessage)
		}
	}
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param f A mapping function to apply to this response's status
	 * @return A copy of this response with a mapped status.
	 *         Note: This response reserves its original success/failure state, regardless of the new status.
	 */
	def mapStatus(f: Mutate[Status]) = withStatus(f(status))
	/**
	 * @param f A mapping function to apply to this response's headers
	 * @return A copy of this response with mapped headers.
	 */
	def mapHeaders(f: Mutate[Headers]) = withHeaders(f(headers))
}

/**
  * A common trait for both success and failure responses
  * @tparam A Response content / parsed body type expected in successful responses
  * @author Mikko Hilpinen
  * @since 14.6.2020, v1
  */
sealed trait Response[+A] extends ResponseLike[A, Response[A], Response]

object Response
{
	// ATTRIBUTES   ---------------------------
	
	/**
	 * Status yielded on response-parsing failures
	 */
	var parseFailureStatus: Status = InternalServerError
	
	
	// VALUES   -------------------------------
	
	/**
	  * Success responses are used when the server successfully appropriates the request
	  * @param value Parsed response value (typically from the response body)
	  * @param status Status returned by the server
	  * @param headers Response headers
	  */
	case class Success[+A](value: A, status: Status = OK, headers: Headers = Headers.empty)
		extends Response[A] with ResponseLike[A, Success[A], Response]
	{
		// ATTRIBUTES   ---------------------
		
		override val isSuccess = true
		
		
		// IMPLEMENTED  ---------------------
		
		override def success: Option[A] = Some(value)
		override def failure: Option[Throwable] = None
		
		override def get: A = value
		
		override def toTry = scala.util.Success(value)
		override def toTryCatch: TryCatch[A] = TryCatch.Success(value)
		override def toString = s"$status: $value"
		
		override def withStatus(status: Status): Success[A] = copy(status = status)
		override def withHeaders(headers: Headers): Success[A] = copy(headers = headers)
		override def toSuccessWithValue[B](newValue: B): Success[B] = withValue(newValue)
		
		override def map[B](f: A => B) = copy(value = f(value))
		override def flatMap[B](f: A => Either[String, B]): Response[B] = f(value) match {
			case Right(newValue) => copy(value = newValue)
			case Left(message) => Failure(parseFailureStatus, message, headers)
		}
		override def mapOrFail[B](f: A => MayHaveFailed[B]): Response[B] = f(value) match {
			case r: Response[B] => r
			case failure: MayHaveFailed.Failure =>
				Response.Failure(parseFailureStatus, Option(failure.cause.getMessage).getOrElse(""), headers)
			case result =>
				result.toTry match {
					case scala.util.Success(newValue) => withValue(newValue)
					case scala.util.Failure(error) =>
						Response.Failure(parseFailureStatus, Option(error.getMessage).getOrElse(""), headers)
				}
		}
		
		override def tryMap[B](f: A => Try[B]): Response[B] = tryMap(parseFailureStatus)(f)
		override def tryMapCatching[B](f: A => TryCatch[B]): TryCatch[B] = f(value)
		
		override def catching[B >: A](partialFailures: => IterableOnce[Throwable]): TryCatch[B] =
			TryCatch.Success(value, OptimizedIndexedSeq.from(partialFailures))
		
		
		// OTHER    ------------------------
		
		/**
		 * @param newValue New value to assign to this success
		 * @tparam B Type of the new value
		 * @return A copy of this success containing the specified value
		 */
		def withValue[B](newValue: B) = copy(value = newValue)
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
		extends Response[Nothing] with RequestFailure with ResponseLike[Nothing, Failure, Response]
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
		override def flatMap[B](f: Nothing => Either[String, B]) = this
		override def tryMap[B](f: Nothing => Try[B]) = this
		override def tryMap[B](parseFailureStatus: => Status)(f: Nothing => Try[B]) = this
		override def mapOrFail[B](f: Nothing => MayHaveFailed[B]) = this
		
		override def toSuccessWithValue[B](newValue: B): Success[B] = Response.Success(newValue, status, headers)
		
		override def withStatus(status: Status): Failure = copy(status = status)
		override def withHeaders(headers: Headers): Failure = copy(headers = headers)
	}
}
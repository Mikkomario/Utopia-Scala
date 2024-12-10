package utopia.annex.util

import utopia.access.http.Status
import utopia.annex.controller.PreparingResponseParser
import utopia.annex.model.response.Response
import utopia.disciple.http.response.{ResponseParseResult, ResponseParser}
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.logging.{Logger, NoOpLogger}

import scala.util.{Failure, Success, Try}

/**
  * Provides additional methods related to response parsing
  * @author Mikko Hilpinen
  * @since 14.07.2024, v1.8
  */
object ResponseParseExtensions
{
	implicit class ExtendedResponseParser[A](val p: ResponseParser[A]) extends AnyVal
	{
		/**
		  * Converts this parser to an Annex-compatible response parser. Will not modify response body value.
		  * @param extractErrorMessage A function which extracts an error message from the response body value.
		  *                            Only called for failure responses (4XX-5XX)
		  * @return Copy of this parser which converts the result into a success or a failure response based on
		  *         the response status
		  */
		def toResponse(extractErrorMessage: A => String) =
			PreparingResponseParser.wrap(p)(extractErrorMessage)
		
		/**
		  * Converts this parser to an Annex-compatible response parser.
		  * Performs a final mapping, in case the response is a success.
		  * @param f A mapping function called for successful responses, finalizing their body value
		  * @param extractErrorMessage A function which extracts an error message from the response body value.
		  *                            Only called for failure responses (4XX-5XX)
		  * @return Copy of this parser which converts the result into a success or a failure response based on
		  *         the response status
		  */
		def mapToResponse[B](f: ResponseParseResult[A] => B)(extractErrorMessage: A => String) =
			PreparingResponseParser.map(p)(f)(extractErrorMessage)
		
		/**
		  * Converts this parser to an Annex-compatible response parser.
		  * Performs a final mapping, in case the response is a success.
		  * This final mapping may still convert the response into a failure response.
		  * @param f A mapping function called for successful responses, finalizing their body value.
		  *          Returns either:
		  *             - Right: If this response should be preserved as a success (containing a mapping result)
		  *             - Left: Status + message if this response should be converted into a failure response instead
		  * @param extractErrorMessage A function which extracts an error message from the response body value.
		  *                            Only called for failure responses (4XX-5XX)
		  * @return Copy of this parser which converts the result into a success or a failure response based on
		  *         the response status and possibly the specified mapping function
		  */
		def mapToResponseOrFail[B](f: ResponseParseResult[A] => Either[(Status, String), B])
		                          (extractErrorMessage: A => String) =
			PreparingResponseParser.mapOrFail(p)(f)(extractErrorMessage)
		/**
		  * Converts this parser to an Annex-compatible response parser.
		  * Performs a final mapping, in case the response is a success.
		  * This final mapping may still convert the response into a failure response.
		  * @param parseFailureStatus Status assigned for failure responses resulting from parsing failures
		  *                           (call-by-name)
		  * @param f A mapping function called for successful responses, finalizing their body value.
		  *          Returns a failure if the response should be converted into a failure response.
		  * @param extractErrorMessage A function which extracts an error message from the response body value.
		  *                            Only called for failure responses (4XX-5XX)
		  * @return Copy of this parser which converts the result into a success or a failure response based on
		  *         the response status and possibly the specified mapping function
		  */
		def tryMapToResponse[B](parseFailureStatus: => Status)(f: ResponseParseResult[A] => Try[B])
		                       (extractErrorMessage: A => String): PreparingResponseParser[A, B] =
			mapToResponseOrFail { f(_) match {
				case Success(r) => Right(r)
				case Failure(error) => Left(parseFailureStatus -> error.getMessage)
			} }(extractErrorMessage)
	}
	
	implicit class TryToResponseParser[A](val p: ResponseParser[Try[A]]) extends AnyVal
	{
		/**
		  * Converts this parser to an Annex-compatible response parser.
		  * Converts response body parse failures to failed responses.
		  * @param parseFailureStatus Status to assign for response-parsing failures
		  * @param extractErrorMessage A function which extracts an error message from the response body value.
		  *                            Only called for failure responses (4XX-5XX) which contain a successful value.
		  * @return Copy of this parser which converts the result into a success or a failure response based on
		  *         the response status and the response body parsing success or failure
		  */
		def unwrapToResponse(parseFailureStatus: => Status)(extractErrorMessage: A => String) =
			unwrapToResponseLogging(parseFailureStatus)(extractErrorMessage)(NoOpLogger)
		/**
		 * Converts this parser to an Annex-compatible response parser.
		 * Converts response body parse failures to failed responses. Logs encountered failures.
		 * @param parseFailureStatus Status to assign for response-parsing failures
		 * @param extractErrorMessage A function which extracts an error message from the response body value.
		 *                            Only called for failure responses (4XX-5XX) which contain a successful value.
		 * @param log Implicit logging implementation used for recording the encountered errors
		 * @return Copy of this parser which converts the result into a success or a failure response based on
		 *         the response status and the response body parsing success or failure
		 */
		def unwrapToResponseLogging(parseFailureStatus: => Status)(extractErrorMessage: A => String)
		                           (implicit log: Logger) =
			p.mapToResponseOrFail[A] { r =>
				r.wrapped match {
					case Success(body) => Right(body)
					case Failure(error) =>
						log(error, "Failed to parse a successful response")
						Left(parseFailureStatus -> error.getMessage)
				}
			} {
				case Success(body) => extractErrorMessage(body)
				case Failure(error) =>
					log(error, "A failed request")
					error.getMessage
			}
		
		/**
		  * Flat-maps and converts this parser to an Annex-compatible response parser.
		  * Converts response body parse failures and mapping failures to failed responses.
		  * @param parseFailureStatus Status to assign for response-parsing failures
		  * @param f A mapping function applied to successfully parsed values.
		  *          Yields either a mapped value or a failure.
		  * @param extractErrorMessage A function which extracts an error message from the unmapped response body value.
		  *                            Only called for failure responses (4XX-5XX) which contain a successful value.
		  * @return Copy of this parser which converts the result into a success or a failure response based on
		  *         the response status and the response body parsing success or failure
		  */
		def tryFlatMapToResponse[B](parseFailureStatus: => Status)(f: A => Try[B])(extractErrorMessage: A => String) =
			p.mapToResponseOrFail { r =>
				r.wrapped.flatMap(f) match {
					case Success(parsed) => Right(parsed)
					case Failure(error) => Left(parseFailureStatus -> error.getMessage)
				}
			} {
				case Success(body) => extractErrorMessage(body)
				case Failure(error) => error.getMessage
			}
	}
	
	implicit class EitherToResponseParser[S, F](val p: ResponseParser[Either[F, S]]) extends AnyVal
	{
		/**
		  * Converts this response-parser into an Annex-compatible response parser
		  * @param parseFailureStatus (Failure) response status used for parsing failures
		  * @param failureToErrorMessage Extracts an error message from a left (i.e. failure) side item
		  * @param successToErrorMessage Extracts an error message from a right (i.e. success) side item.
		  *                              Only called if the response status indicates a failure (i.e. is 4XX or 5XX)
		  * @return A response parser that yields the right-side value on success
		  */
		def rightToResponse(parseFailureStatus: => Status)
		                   (failureToErrorMessage: F => String)
		                   (successToErrorMessage: S => String) =
			p.mapToResponseOrFail { _.wrapped.mapLeft { parseFailureStatus -> failureToErrorMessage(_) } } {
				case Right(s) => successToErrorMessage(s)
				case Left(f) => failureToErrorMessage(f)
			}
	}
	
	implicit class AnnexResponseParser[+A](val p: ResponseParser[Response[A]]) extends AnyVal
	{
		/**
		  * @param f A mapping function to apply to successful response contents
		  * @tparam B Type of mapped contents
		  * @return Copy of this parser which applies the specified mapping function to successful responses
		  */
		def mapSuccess[B](f: A => B) = p.map { _.map(f) }
		
		/**
		  * @param f A mapping function to apply to successful response contents,
		  *          possibly converting it to a failure response instead.
		  *          Yields either:
		  *             - Right: Successful mapping result
		  *             - Left: Failure status + failure message to assign to a new failure response
		  * @tparam B Type of successful mapping result
		  * @return Copy of this parser which applies the specified mapping function in addition to normal parsing logic
		  */
		def tryMapSuccess[B](f: A => Either[(Status, String), B]) = p.map { _.tryMap(f) }
	}
}

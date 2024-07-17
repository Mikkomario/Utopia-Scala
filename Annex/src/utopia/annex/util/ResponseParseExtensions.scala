package utopia.annex.util

import utopia.access.http.Status
import utopia.annex.controller.PreparingResponseParser
import utopia.annex.model.response.Response
import utopia.disciple.http.response.{ResponseParseResult, ResponseParser}

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
		def toResponseWith(extractErrorMessage: A => String) =
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
		def mapToResponseWith[B](f: ResponseParseResult[A] => B)(extractErrorMessage: A => String) =
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
		def tryMapToResponseWith[B](f: ResponseParseResult[A] => Either[(Status, String), B])
		                           (extractErrorMessage: A => String) =
			PreparingResponseParser.tryMap(p)(f)(extractErrorMessage)
	}
	
	implicit class TryResponseParser[A](val p: ResponseParser[Try[A]]) extends AnyVal
	{
		/**
		  * Converts this parser to an Annex-compatible response parser.
		  * Converts response body parse failures to failed responses.
		  * @param parseFailureStatus Status to assign for response-parsing failures
		  * @param extractErrorMessage A function which extracts an error message from the response body value.
		  *                            Only called for failure responses (4XX-5XX)
		  * @return Copy of this parser which converts the result into a success or a failure response based on
		  *         the response status and the response body parsing success or failure
		  */
		def unwrapToResponseWith(parseFailureStatus: Status)(extractErrorMessage: A => String) =
			p.tryMapToResponseWith[A] { _.wrapped match {
					case Success(body) => Right(body)
					case Failure(error) => Left(parseFailureStatus -> error.getMessage)
				}
			} {
				case Success(body) => extractErrorMessage(body)
				case Failure(error) => error.getMessage
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

package utopia.annex.controller

import utopia.access.http.{Headers, Status}
import utopia.annex.model.response.Response
import utopia.disciple.http.response.{ResponseParseResult, ResponseParser}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.Identity

import java.io.InputStream
import scala.util.{Failure, Success, Try}

object PreparingResponseParser
{
	// OTHER    --------------------------
	
	/**
	  * Creates a new response parser by wrapping another and applying a finalization function to it
	  * @param primary Response parser to post-process
	  * @param f A function which accepts three values:
	  *             1. Parse result from the primary response parser
	  *             1. Response status
	  *             1. Response headers
	  *
	  *          And yields a fully parsed response
	  *          (either a success or a failure, typically based on the response status).
	  * @tparam M Type of the primary parse results
	  * @tparam A Type of final successful response values
	  * @return A new response parser which post-processes the specified parser's results
	  */
	def apply[M, A](primary: ResponseParser[M])
	               (f: (ResponseParseResult[M], Status, Headers) => Response[A]): PreparingResponseParser[M, A] =
		new _PreparingResponseParser[M, A](primary, f)
	
	/**
	  * Creates a new response parser which either maps the results of another parser, if successful,
	  * or extracts an error message from the results of another parser,
	  * if the response was a failure response (4XX-5XX).
	  * @param primary Response parser which pre-processes the response body
	  * @param mapSuccess A function called for successful responses,
	  *                   which maps the response body into another data type
	  * @param extractFailureMessage A function called for failure responses (4XX-5XX),
	  *                              which extracts an error message from the response body
	  * @tparam M Type of the preliminary parse results
	  * @tparam A Type of the final mapping results
	  * @return A response parser which categorizes the responses based on their status and applies either
	  *         the specified mapping function, or extracts an error message using the specified extraction function.
	  */
	def map[M, A](primary: ResponseParser[M])
	             (mapSuccess: ResponseParseResult[M] => A)(extractFailureMessage: M => String) =
		apply(primary) { (body, status, headers) =>
			if (status.isFailure)
				Response.Failure(status, extractFailureMessage(body), headers)
			else
				Response.Success(mapSuccess(body), status, headers)
		}
	
	/**
	  * Creates a new response parser which either maps the results of another parser, if successful,
	  * or extracts an error message from the results of another parser,
	  * if the response was a failure response (4XX-5XX).
	  *
	  * The success case may be converted into a failure by the specified mapping function.
	  *
	  * @param primary Response parser which pre-processes the response body
	  * @param mapSuccess A function called for successful responses,
	  *                   which maps the response body into either:
	  *                         - Right: Another data type for a successful response
	  *                         - Left: A new response status + an error message,
	  *                         in case the response needs to be converted into a failure response instead
	  * @param extractFailureMessage A function called for failure responses (4XX-5XX),
	  *                              which extracts an error message from the response body
	  * @tparam M Type of the preliminary parse results
	  * @tparam A Type of the final mapping results
	  * @return A response parser which categorizes the responses based on their status and applies either
	  *         the specified mapping function, or extracts an error message using the specified extraction function.
	  */
	def mapOrFail[M, A](primary: ResponseParser[M])(mapSuccess: ResponseParseResult[M] => Either[(Status, String), A])
	                   (extractFailureMessage: M => String) =
		apply(primary) { (body, status, headers) =>
			if (status.isFailure)
				Response.Failure(status, extractFailureMessage(body), headers)
			else
				mapSuccess(body) match {
					case Right(success) => Response.Success(success, status, headers)
					case Left((newStatus, failureMessage)) => Response.Failure(newStatus, failureMessage, headers)
				}
		}
	/**
	  * Creates a new response parser which either maps the results of another parser, if successful,
	  * or extracts an error message from the results of another parser,
	  * if the response was a failure response (4XX-5XX).
	  *
	  * The success case may be converted into a failure by the specified mapping function.
	  *
	  * @param primary Response parser which pre-processes the response body
	  * @param parseFailureStatus Status assigned in case a successful response is converted to a failure response
	  *                           because of a parsing failure.
	  * @param mapSuccess A mapping function called for successful responses.
	  *                   May yield a failure, which converts the response into a failure response.
	  * @param extractFailureMessage A function called for failure responses (4XX-5XX),
	  *                              which extracts an error message from the response body
	  * @tparam M Type of the preliminary parse results
	  * @tparam A Type of the final mapping results
	  * @return A response parser which categorizes the responses based on their status and applies either
	  *         the specified mapping function, or extracts an error message using the specified extraction function.
	  */
	def tryMap[M, A](primary: ResponseParser[M], parseFailureStatus: => Status)
	                (mapSuccess: ResponseParseResult[M] => Try[A])
	                (extractFailureMessage: M => String): PreparingResponseParser[M, A] =
		mapOrFail[M, A](primary) { mapSuccess(_) match {
			case Success(v) => Right(v)
			case Failure(error) => Left(parseFailureStatus -> error.getMessage)
		} }(extractFailureMessage)
	
	/**
	  * Converts StreamedResponses to Annex Responses by simply wrapping another response parser
	  * @param parser A response parser to wrap
	  * @param extractFailureMessage A function which extracts a failure message from the parsed response body,
	  *                              in case of a failure response (4XX-5XX)
	  * @tparam A Type of the parsed response body values
	  * @return A new response parser which uses the specified parser to parse the response body and checks
	  *         the response status for whether to consider it a success or a failure response
	  */
	def wrap[A](parser: ResponseParser[A])(extractFailureMessage: A => String) =
		map(parser) { _.wrapped }(extractFailureMessage)
	
	/**
	  * Creates a response parser which is only interested in error handling.
	  * Bodies of successful responses are ignored.
	  * @param failureParser A parser used for converting failure response bodies into the intermediate data type
	  * @param extractFailureMessage A function for extracting an error message from the intermediate data type
	  * @tparam A Type of the intermediate state for failure responses
	  * @return A response parser which ignores successes, but converts failures with a two-step-process
	  */
	def onlyMapFailures[A](failureParser: ResponseParser[A])(extractFailureMessage: A => String) =
		map[Either[A, Unit], Unit](
			ResponseParser.static[Either[A, Unit]](Right(()))
				.handleFailuresUsing(failureParser.map { Left(_) })) {
			_ => () } {
			_.leftOption match {
				case Some(a) => extractFailureMessage(a)
				case None => ""
			}
		}
	
	/**
	  * Creates a response parser which is only interested in error handling.
	  * Bodies of successful responses are ignored.
	  * @param errorMessageParser A response parser applied to failure responses only.
	  *                           Extracts the failure message.
	  * @return A response parser which ignores successes, but parses error messages from failures
	  */
	def onlyRecordFailures(errorMessageParser: ResponseParser[String]) =
		map[String, Unit](ResponseParser.static("").handleFailuresUsing(errorMessageParser)) { _ => () }(Identity)
	
	
	// NESTED   --------------------------
	
	private class _PreparingResponseParser[M, +A](override val primaryParser: ResponseParser[M],
	                                              f: (ResponseParseResult[M], Status, Headers) => Response[A])
		extends PreparingResponseParser[M, A]
	{
		override protected def finalize(prepared: ResponseParseResult[M], status: Status, headers: Headers) =
			f(prepared, status, headers)
	}
}

/**
  * Used for converting [[utopia.disciple.http.response.StreamedResponse]]s to
  * [[utopia.annex.model.response.Response]]s by utilizing a preparing [[ResponseParser]]
  * @author Mikko Hilpinen
  * @since 14.07.2024, v1.8
  */
trait PreparingResponseParser[M, +A] extends ResponseParser[Response[A]]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @return A parser used for preprocessing the response,
	  *         regardless of whether it is a success or a failure response.
	  */
	protected def primaryParser: ResponseParser[M]
	
	/**
	  * Finalizes the parsing process by either converting the result into a success or a failure
	  * @param prepared Pre-parsed response body
	  * @param status Response status
	  * @param headers Response headers
	  * @return Either a success or a failure response
	  */
	protected def finalize(prepared: ResponseParseResult[M], status: Status, headers: Headers): Response[A]
	
	
	// IMPLEMENTED  --------------------------
	
	override def apply(status: Status, headers: Headers, stream: Option[InputStream]) = {
		val primaryResult = primaryParser(status, headers, stream)
		ResponseParseResult(finalize(primaryResult, status, headers), primaryResult.parseCompletion)
	}
}

package utopia.echo.controller.parser

import utopia.access.http.{Headers, Status}
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.http.response.{ResponseParseResult, ResponseParser}
import utopia.echo.controller.EchoContext
import utopia.flow.generic.model.immutable.Model
import utopia.flow.operator.Identity
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.IterateLines
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger

import java.io.InputStream
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Codec
import scala.util.{Success, Try}

/**
  * A response parser which parses streamed json responses.
  * Expects the stream to contain newline-delimited json where each line represents a json object.
  * @tparam R Type of the (streamed) responses parsed
  * @tparam V Type of asynchronously acquired value utilized
  * @author Mikko Hilpinen
  * @since 3.9.2024, v1.1
  */
trait StreamedResponseParser[R, V] extends ResponseParser[R]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Implicit execution context used in asynchronous parsing
	  */
	protected implicit def exc: ExecutionContext
	/**
	  * @return Json parser used
	  */
	protected implicit def jsonParser: JsonParser
	/**
	  * @return Implicit logging implementation used
	  */
	protected implicit def log: Logger
	
	/**
	  * @return Response returned when no response body is read
	  */
	protected def emptyResponse: R
	/**
	  * @return A new parser for processing a received non-empty response
	  */
	protected def newParser: SingleStreamedResponseParser[R, V]
	
	/**
	  * Converts the body of this a response into a failure message.
	  * Called only for failure responses (i.e. 4XX-5XX status responses)
	  * (and really to for even those).
	  * @param response An acquired or building response
	  * @return A failure message extracted from that response
	  */
	protected def failureMessageFrom(response: R): String
	
	
	// COMPUTED   -------------------------
	
	private implicit def codec: Codec = Codec.UTF8
	
	/**
	  * Copy of this parser which processes the replies into full responses,
	  * handling failures & failure responses, also.
	  */
	def toResponse =
		toRight(ResponseParser.string.map { _.getOrMap { _.getMessage } })
			.rightToResponse(EchoContext.parseFailureStatus)(Identity)(failureMessageFrom)
	
	
	// IMPLEMENTED  -------------------------
	
	// TODO: Add support for interrupting the streaming, here
	/*
	Note (from HttpClient getContent function): If this entity belongs to an incoming HTTP message,
	calling InputStream. close()   on the returned InputStream will try to consume the complete entity content
	to keep the connection alive. In cases where this is undesired, e. g. when only a small part of the content
	is relevant and consuming the complete entity content would be too inefficient, only the HTTP message from which
	this entity was obtained should be closed (if supported).
	 */
	override def apply(status: Status, headers: Headers, stream: Option[InputStream]) = {
		stream match {
			// Case: Streamed response => Starts parsing the stream json contents
			case Some(stream) =>
				// Prepares a pointer to store the read reply text
				val parser = newParser
				
				// Starts reading the stream asynchronously
				val resultFuture = Future {
					// Reads one line at a time. Expects each line to contain a json object.
					val result = IterateLines
						.fromStream(stream) { linesIter =>
							// Stores the latest read line / model for the final result -parsing
							var lastResult: Try[Model] = Success(Model.empty)
							// Parses each line to a model
							linesIter.map { line => jsonParser(line).flatMap { _.tryModel } }.foreach { parseResult =>
								lastResult = parseResult
								// Whenever a parsing succeeds, updates the parser state
								parseResult.foreach(parser.updateStatus)
							}
							// Processes the last read model or parse failure
							parser.processFinalParseResult(lastResult)
						}
						.flatten
					
					// Informs the parser that the process has completed - There shall not be any updates afterwards
					parser.finish()
					
					result
				}
				
				// Returns the acquired response, as it builds in another thread
				ResponseParseResult(parser.responseFrom(resultFuture), resultFuture)
				
			// Case: Empty response
			case None => ResponseParseResult.buffered(emptyResponse)
		}
	}
}

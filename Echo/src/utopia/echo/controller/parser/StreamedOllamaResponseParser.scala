package utopia.echo.controller.parser

import utopia.access.http.{Headers, Status}
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.http.response.{ResponseParseResult, ResponseParser}
import utopia.echo.controller.EchoContext
import utopia.echo.model.response.{OllamaResponse, ResponseStatistics}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.IterateLines
import utopia.flow.time.Now
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.LockablePointer
import utopia.flow.view.template.eventful.Changing

import java.io.InputStream
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Codec
import scala.util.{Success, Try}

/**
  * A response parser which parses LLM replies from a streamed json response.
  * Expects the stream to contain newline-delimited json where each line represents a json object.
  * @tparam A Type of the (streamed) responses parsed
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
trait StreamedOllamaResponseParser[A <: OllamaResponse[_]] extends ResponseParser[A]
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
	protected def emptyResponse: A
	
	/**
	  * @param response Response model to read
	  * @return Incremented text from the specified response
	  */
	protected def textFromResponse(response: Model): String
	
	/**
	  * @param textPointer Pointer that will contain all response text (built incrementally)
	  * @param lastUpdatedPointer Pointer that contains the origin time of the latest response update
	  * @param statisticsFuture Future that eventually resolves into statistics concerning this response,
	  *                         or to a failure if response or json processing fails.
	  * @return Completed streamed response instance
	  */
	protected def responseFrom(textPointer: Changing[String], lastUpdatedPointer: Changing[Instant],
	                           statisticsFuture: Future[Try[ResponseStatistics]]): A
	
	
	// COMPUTED   -------------------------
	
	private implicit def codec: Codec = Codec.UTF8
	
	/**
	  * Copy of this parser which processes the replies into full responses,
	  * handling failures & failure responses, also.
	  */
	def toResponse =
		toRight(ResponseParser.string.map { _.getOrMap { _.getMessage } })
			.mapToResponseOrFail { _.wrapped.mapLeft { EchoContext.parseFailureStatus -> _ } } {
				_.leftOrMap { _.textPointer.value } }
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(status: Status, headers: Headers, stream: Option[InputStream]) = {
		stream match {
			// Case: Streamed response => Starts parsing the stream json contents
			case Some(stream) =>
				// Prepares a pointer to store the read reply text
				// TODO: May need Volatile features here
				val textPointer = new LockablePointer[String]("")
				val lastUpdatedPointer = new LockablePointer[Instant](Now)
				
				// Starts reading the stream asynchronously
				val statisticsFuture = Future {
					// Reads one line at a time. Expects each line to contain a json object.
					val result = IterateLines
						.fromStream(stream) { linesIter =>
							// Stores the latest read line / model for the final result -parsing
							var lastResult: Try[Model] = Success(Model.empty)
							// Parses each line to a model
							linesIter.map { line => jsonParser(line).flatMap { _.tryModel } }.foreach { parseResult =>
								lastResult = parseResult
								// From successful parsings, acquires the response text and the last update time value
								parseResult.foreach { responseModel =>
									val newText = textFromResponse(responseModel)
									// Appends the read text to the text pointer
									textPointer.update { _ + newText }
									lastUpdatedPointer.value = responseModel("created_at").getInstant
								}
							}
							// Converts the last read model into response statistics, if possible
							lastResult.map(ResponseStatistics.fromOllamaResponse)
						}
						.flatten
					
					// Locks the pointers - There shall not be any updates afterwards
					textPointer.lock()
					lastUpdatedPointer.lock()
					
					result
				}
				
				// Returns the acquired reply (building)
				ResponseParseResult(responseFrom(textPointer.readOnly, lastUpdatedPointer.readOnly, statisticsFuture),
					statisticsFuture)
				
			// Case: Empty response => Returns an empty reply
			case None => ResponseParseResult.buffered(emptyResponse)
		}
	}
}

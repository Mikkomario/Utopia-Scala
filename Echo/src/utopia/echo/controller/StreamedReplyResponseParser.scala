package utopia.echo.controller

import utopia.access.http.{Headers, Status}
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.http.response.{ResponseParseResult, ResponseParser}
import utopia.echo.model.response.{ResponseStatistics, StreamedReply}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.string.IterateLines
import utopia.flow.time.Now
import utopia.flow.view.mutable.eventful.LockablePointer

import java.io.InputStream
import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.io.Codec
import scala.util.{Success, Try}

/**
  * A response parser which parses LLM replies from a streamed json response.
  * Expects the stream to contain a sequence of models separated from each other by whitespace.
  * @param exc Implicit execution context
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
class StreamedReplyResponseParser(implicit exc: ExecutionContext, jsonParser: JsonParser)
	extends ResponseParser[StreamedReply]
{
	// ATTRIBUTES   -------------------------
	
	private implicit val codec: Codec = Codec.UTF8
	
	/**
	  * Copy of this parser which processes the replies into full responses,
	  * handling failures & failure responses, also.
	  */
	lazy val toResponse = toRight(ResponseParser.string.map { _.getOrMap { _.getMessage } })
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
									responseModel("response").string.foreach { newText =>
										// Appends the read text to the text pointer
										textPointer.update { _ + newText }
									}
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
				ResponseParseResult(
					StreamedReply(textPointer.readOnly, lastUpdatedPointer.readOnly, statisticsFuture),
					statisticsFuture)
				
			// Case: Empty response => Returns an empty reply
			case None => ResponseParseResult.buffered(StreamedReply.empty)
		}
	}
}

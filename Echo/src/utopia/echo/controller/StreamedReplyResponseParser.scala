package utopia.echo.controller

import utopia.access.http.{Headers, Status}
import utopia.annex.util.ResponseParseExtensions._
import utopia.bunnymunch.jawn.AsyncJsonBunny
import utopia.disciple.http.response.{ResponseParseResult, ResponseParser}
import utopia.echo.model.response.{ResponseStatistics, StreamedReply}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.time.Now
import utopia.flow.util.StringExtensions._
import utopia.flow.view.mutable.eventful.LockablePointer

import java.io.InputStream
import java.time.Instant
import scala.concurrent.{ExecutionContext, Promise}
import scala.util.{Failure, Success, Try}

/**
  * A response parser which parses LLM replies from a streamed json response.
  * Expects the stream to contain a sequence of models separated from each other by whitespace.
  * @param exc Implicit execution context
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
class StreamedReplyResponseParser(implicit exc: ExecutionContext)
	extends ResponseParser[StreamedReply]
{
	// ATTRIBUTES   -------------------------
	
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
				// Will be completed with Failure on read/parse failure
				val statisticsPromise = Promise[Try[ResponseStatistics]]()
				
				// Starts reading json data from the stream
				val resultFuture = AsyncJsonBunny.processStreamedValues(stream) { values =>
					val models = values.map { _.getModel }
					// Updates the text pointer based on "response" values
					models.map { _("response").getString }.mkString.ifNotEmpty.foreach { newText =>
						textPointer.update { _ + newText }
					}
					// Checks for other statistics (from the final response)
					models.lastOption.foreach { model =>
						if (model("done").getBoolean)
							statisticsPromise.success(Success(ResponseStatistics.fromOllamaResponse(model)))
						lastUpdatedPointer.value = model("created_at").getInstant
					}
				}
				
				// Processes the final read result once reading completes
				resultFuture.foreachResult { result =>
					// Locks the pointers - There shall not be any updates afterwards
					textPointer.lock()
					lastUpdatedPointer.lock()
					
					// Handles the potential failure (unless reading was completed successfully already)
					if (!statisticsPromise.isCompleted) {
						result match {
							// Case: No final statistics -object was received, marks it as a failure
							case Success(_) =>
								statisticsPromise.trySuccess(Failure(new NoSuchElementException(
									"No final response was ever received")))
							case Failure(error) => statisticsPromise.trySuccess(Failure(error))
						}
					}
				}
				
				// Returns the acquired reply (building)
				ResponseParseResult(
					StreamedReply(textPointer.readOnly, lastUpdatedPointer.readOnly, statisticsPromise.future),
					resultFuture)
				
			// Case: Empty response => Returns an empty reply
			case None => ResponseParseResult.buffered(StreamedReply.empty)
		}
	}
}

package utopia.echo.controller.parser

import utopia.echo.model.response.ollama.{BufferedOllamaReply, OllamaReply, OllamaResponseStatistics}
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.SettableFlag

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object StreamedOllamaResponseParser
{
	// COMPUTED --------------------------
	
	/**
	 * @param exc Implicit execution context
	 * @param jsonParser Implicit JSON parser used
	 * @param log Implicit logging interface
	 * @return A response parser for streamed chat requests
	 */
	def chat(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger): StreamedOllamaResponseParser =
		new ChatResponseParser()
	/**
	 * @param exc Implicit execution context
	 * @param jsonParser Implicit JSON parser used
	 * @param log Implicit logging interface
	 * @return A response parser for streamed generate requests
	 */
	def generate(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger): StreamedOllamaResponseParser =
		new GenerateResponseParser()
	
	
	// NESTED   --------------------------
	
	private class ChatResponseParser(implicit override val exc: ExecutionContext,
	                                 override val jsonParser: JsonParser, override val log: Logger)
		extends StreamedOllamaResponseParser
	{
		override protected def textFromResponse(response: Model) = {
			// response("message")("content").getString
			response("message").model match {
				case Some(message) =>
					message("content").string match {
						case Some(text) => text -> false
						case None => message("thinking").getString -> true
					}
				case None => "" -> true
			}
		}
	}
	
	private class GenerateResponseParser(implicit override val exc: ExecutionContext,
	                                     override val jsonParser: JsonParser, override val log: Logger)
		extends StreamedOllamaResponseParser
	{
		// NB: This version doesn't support thinking mode, yet
		override protected def textFromResponse(response: Model) =
			response("response").getString -> false
	}
}

/**
  * A response parser which parses LLM replies from a streamed JSON response.
  * Expects the stream to contain newline-delimited JSON where each line represents a JSON object.
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
trait StreamedOllamaResponseParser extends StreamedNdJsonResponseParser[OllamaReply, OllamaResponseStatistics]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @param response Response model to read
	  * @return Incremented text from the specified response.
	 *         Also returns a boolean indicating whether the read text is part of the model's "thinking" output.
	  */
	protected def textFromResponse(response: Model): (String, Boolean)
	
	
	// IMPLEMENTED  -------------------------
	
	override protected def emptyResponse: OllamaReply = BufferedOllamaReply.empty
	
	override protected def newParser: SingleStreamedResponseParser[OllamaReply, OllamaResponseStatistics] =
		new SingleOllamaResponseParser
	
	override protected def failureMessageFrom(response: OllamaReply): String = response.text
	
	
	// NESTED   ------------------------
	
	private class SingleOllamaResponseParser
		extends SingleStreamedResponseParser[OllamaReply, OllamaResponseStatistics]
	{
		// ATTRIBUTES   ----------------
		
		// Prepares pointers to store the read reply text
		private val newTextPointer = Volatile.lockable("")
		private val textPointer = Volatile.lockable("")
		private val thoughtsPointer = Volatile.lockable("")
		private val thinkingCompletionFlag = SettableFlag()
		private val lastUpdatedPointer = Volatile.lockable[Instant](Now)
		
		
		// IMPLEMENTED  ----------------
		
		override def updateStatus(response: Model): Unit = {
			val (newText, thinking) = textFromResponse(response)
			
			// Updates the thinking flag
			if (!thinking && thinkingCompletionFlag.isNotSet) {
				// Clears the new text -pointer, also,
				// so that the following content will be clearly in non-thinking mode
				newTextPointer.value = ""
				thinkingCompletionFlag.set()
				thoughtsPointer.lock()
			}
			
			// Checks for edge cases, where the new addition is identical to the latest one
			// In these cases, clears the new text -value first, so that a change event will be generated
			// This is not needed if nobody is listening to the new text -pointer
			if (newTextPointer.hasListeners && newText == newTextPointer.value)
				newTextPointer.value = ""
			newTextPointer.value = newText
			
			// Appends the read text to the text or thoughts -pointer
			val buildingPointer = if (thinking) thoughtsPointer else textPointer
			buildingPointer.update { _ + newText }
			lastUpdatedPointer.value = response("created_at").getInstant
		}
		
		// Converts the last read model into response statistics, if possible
		override def processFinalParseResult(finalResponse: Try[Model]): Try[OllamaResponseStatistics] =
			finalResponse.map(OllamaResponseStatistics.fromOllamaResponse)
		
		override def finish(): Unit = {
			// Locks the pointers - There shall not be any updates afterwards
			newTextPointer.lock()
			textPointer.lock()
			thoughtsPointer.lock()
			lastUpdatedPointer.lock()
			thinkingCompletionFlag.set()
		}
		
		override def responseFrom(future: Future[Try[OllamaResponseStatistics]]): OllamaReply =
			OllamaReply(textPointer.readOnly, thoughtsPointer.readOnly, newTextPointer.readOnly,
				!thinkingCompletionFlag, lastUpdatedPointer.readOnly)
				.futureStatistics(future)
	}
}

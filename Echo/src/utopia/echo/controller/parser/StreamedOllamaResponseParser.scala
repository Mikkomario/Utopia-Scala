package utopia.echo.controller.parser

import utopia.echo.model.response.ollama.{BufferedOllamaReply, OllamaReply, OllamaResponseStatistics}
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile

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
		override protected def textFromResponse(response: Model): String = response("message")("content").getString
	}
	
	private class GenerateResponseParser(implicit override val exc: ExecutionContext,
	                                     override val jsonParser: JsonParser, override val log: Logger)
		extends StreamedOllamaResponseParser
	{
		override protected def textFromResponse(response: Model): String = response("response").getString
	}
}

/**
  * A response parser which parses LLM replies from a streamed json response.
  * Expects the stream to contain newline-delimited json where each line represents a json object.
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
trait StreamedOllamaResponseParser extends StreamedNdJsonResponseParser[OllamaReply, OllamaResponseStatistics]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @param response Response model to read
	  * @return Incremented text from the specified response
	  */
	protected def textFromResponse(response: Model): String
	
	
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
		
		// Prepares a pointer to store the read reply text
		private val newTextPointer = Volatile.lockable("")
		private val textPointer = Volatile.lockable("")
		private val lastUpdatedPointer = Volatile.lockable[Instant](Now)
		
		
		// IMPLEMENTED  ----------------
		
		// TODO: Add support for thinking. The model should contain something like "thinking" (in the message section). Also, "thinking" and "content" may be exclusive with each other.
		override def updateStatus(response: Model): Unit = {
			val newText = textFromResponse(response)
			// Checks for edge cases, where the new addition is identical to the latest one
			// In these cases, clears the new text -value first, so that a change event will be generated
			// This is not needed if nobody is listening to the new text -pointer
			if (newTextPointer.hasListeners && newText == newTextPointer.value)
				newTextPointer.value = ""
			newTextPointer.value = newText
			// Appends the read text to the text pointer
			textPointer.update { _ + newText }
			lastUpdatedPointer.value = response("created_at").getInstant
		}
		
		// Converts the last read model into response statistics, if possible
		override def processFinalParseResult(finalResponse: Try[Model]): Try[OllamaResponseStatistics] =
			finalResponse.map(OllamaResponseStatistics.fromOllamaResponse)
		
		override def finish(): Unit = {
			// Locks the pointers - There shall not be any updates afterwards
			newTextPointer.lock()
			textPointer.lock()
			lastUpdatedPointer.lock()
		}
		
		override def responseFrom(future: Future[Try[OllamaResponseStatistics]]): OllamaReply =
			OllamaReply(textPointer.readOnly, newTextPointer.readOnly, lastUpdatedPointer.readOnly)
				.futureStatistics(future)
	}
}

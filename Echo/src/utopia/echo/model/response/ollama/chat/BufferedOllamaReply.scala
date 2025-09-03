package utopia.echo.model.response.ollama.chat

import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.enumeration.ChatRole.Assistant
import utopia.echo.model.response.ollama.{BufferedOllamaResponse, BufferedOllamaResponseLike, OllamaResponseStatistics}
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.flow.view.template.Extender

import java.time.Instant

object BufferedOllamaReply
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * An empty reply message
	  */
	lazy val empty = apply(Assistant(""), OllamaResponseStatistics.empty)
	
	
	// OTHER    --------------------------
	
	/**
	  * Converts an Ollama API -originated model into a reply message.
	  * Expects the whole message to be present in the specified model.
	  * @param responseModel Response model to parse.
	  *                      Should include statistical information, also.
	  * @return A message read from the specified model
	  */
	def fromOllamaResponse(responseModel: Model) = {
		// TODO: Add logging or return failure for invalid messages
		apply(ChatMessage.parseFrom(responseModel("message").getModel, Assistant),
			OllamaResponseStatistics.fromOllamaResponse(responseModel))
	}
}

/**
  * A reply from an LLM which has been fully read to memory
  * @param message The reply message itself
  * @param statistics Statistics about the response-generation process
  * @param lastUpdated Origin time of the latest version of message's contents
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
case class BufferedOllamaReply(message: ChatMessage, statistics: OllamaResponseStatistics, lastUpdated: Instant = Now)
	extends Extender[ChatMessage] with BufferedOllamaResponse with BufferedOllamaResponseLike[BufferedOllamaReply]
		with OllamaReply
{
	override def self: BufferedOllamaReply = this
	override def wrapped: ChatMessage = message
	
	override def text: String = message.text
	override def thoughts: String = message.thoughts
	override def senderRole: ChatRole = message.senderRole
}

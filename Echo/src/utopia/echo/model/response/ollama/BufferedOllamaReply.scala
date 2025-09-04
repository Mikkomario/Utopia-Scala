package utopia.echo.model.response.ollama

import utopia.echo.model.ChatMessage
import utopia.echo.model.enumeration.ChatRole.Assistant
import utopia.echo.model.response.BufferedReply
import utopia.echo.util.ReplyParseUtils
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now

import java.time.Instant

object BufferedOllamaReply
{
	// COMPUTED -----------------------
	
	/**
	 * @return An empty response
	 */
	def empty = apply("", "", OllamaResponseStatistics.empty, Now)
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param text Reply text
	 * @param thoughts Reply thinking content
	 * @param statistics Reply statistics
	 * @param created Time when this (final version of) reply was created
	 * @return A new response containing the specified info
	 */
	def apply(text: String, thoughts: String, statistics: OllamaResponseStatistics,
	          created: Instant = Now): BufferedOllamaReply =
		wrap(ChatMessage(text, thoughts), statistics, created)
	/**
	 * @param message The chat message to wrap
	 * @param statistics Reply statistics
	 * @param created Time when this (final version of) reply was created
	 * @return A new response containing the specified info
	 */
	def wrap(message: ChatMessage, statistics: OllamaResponseStatistics, created: Instant = Now): BufferedOllamaReply =
		_BufferedOllamaReply(message, statistics, created)
	
	/**
	 * Converts an Ollama API -originated /generate endpoint response model into a buffered response.
	 * Should only be called for models where "done" is set to true (i.e. for the final responses).
	 * @param responseModel A response model returned by the Ollama API
	 * @return A buffered response (final) parsed from the response
	 */
	def fromOllamaGenerateResponse(responseModel: Model) = {
		val (text, think) = ReplyParseUtils.separateThinkFrom(responseModel("response").getString)
		apply(text, think,
			OllamaResponseStatistics.fromOllamaResponse(responseModel), responseModel("created_at").getInstant)
	}
	/**
	 * Converts an Ollama API -originated model into a reply message.
	 * Expects the whole message to be present in the specified model.
	 * @param responseModel Response model to parse.
	 *                      Should include statistical information, also.
	 * @return A message read from the specified model
	 */
	def fromOllamaChatResponse(responseModel: Model) = {
		// TODO: Add logging or return failure for invalid messages
		wrap(ChatMessage.parseFrom(responseModel("message").getModel, Assistant),
			OllamaResponseStatistics.fromOllamaResponse(responseModel))
	}
	
	
	// NESTED   ----------------------------
	
	private case class _BufferedOllamaReply(message: ChatMessage, statistics: OllamaResponseStatistics,
	                                        lastUpdated: Instant)
		extends BufferedOllamaReply
	{
		// IMPLEMENTED  -----------------------
		
		override def self: _BufferedOllamaReply = this
		
		override def text: String = message.text
		override def thoughts: String = message.thoughts
	}
}

/**
 * Common trait for buffered ollama responses.
 * @author Mikko Hilpinen
 * @since 12.01.2025, v1.2
 */
trait BufferedOllamaReply
	extends OllamaReply with BufferedReply with BufferedOllamaReplyLike[BufferedOllamaReply]

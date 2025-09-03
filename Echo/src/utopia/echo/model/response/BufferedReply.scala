package utopia.echo.model.response

import utopia.echo.model.ChatMessage
import utopia.flow.time.Now

import java.time.Instant

object BufferedReply
{
	// OTHER    --------------------------
	
	/**
	 * @param message Wrapped chat message
	 * @param tokenUsage Statistics about token usage
	 * @param created Time when this (version of this) response was created
	 * @return A new buffered reply instance
	 */
	def apply(message: ChatMessage, tokenUsage: TokenUsage, created: Instant = Now): BufferedReply =
		_BufferedReply(message, tokenUsage, created)
	
	
	// NESTED   --------------------------
	
	private case class _BufferedReply(message: ChatMessage, tokenUsage: TokenUsage, lastUpdated: Instant)
		extends BufferedReply
}

/**
 * Common trait for chat message -based buffered response classes
 *
 * @author Mikko Hilpinen
 * @since 02.09.2025, v1.4
 */
trait BufferedReply extends BufferedResponse
{
	// ABSTRACT --------------------------
	
	/**
	 * @return The wrapped reply message
	 */
	def message: ChatMessage
	
	
	// IMPLEMENTED  ----------------------
	
	override def text: String = message.text
	override def thoughts: String = message.thoughts
}

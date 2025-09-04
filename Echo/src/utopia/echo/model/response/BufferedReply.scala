package utopia.echo.model.response

import utopia.echo.model.ChatMessage
import utopia.flow.time.Now

import java.time.Instant

object BufferedReply
{
	// OTHER    ------------------------
	
	/**
	 * @param message A chat message to wrap
	 * @param tokenUsage Statistics about token usage
	 * @param created Time when this (version of this) response was created
	 * @return A new response
	 */
	def apply(message: ChatMessage, tokenUsage: TokenUsage = TokenUsage.zero,
	          created: Instant = Now): BufferedReply =
		_BufferedReply(message, tokenUsage, created)
	
	
	// NESTED   ------------------------
	
	private case class _BufferedReply(message: ChatMessage, tokenUsage: TokenUsage, lastUpdated: Instant)
		extends BufferedReply
	{
		override def self: BufferedReply = this
		
		override def text: String = message.text
		override def thoughts: String = message.thoughts
	}
}

/**
 * Common trait for buffered responses, regardless of service provider
 * @author Mikko Hilpinen
 * @since 02.09.2025, v1.4
 */
trait BufferedReply extends Reply with BufferedReplyLike[BufferedReply]
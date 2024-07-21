package utopia.echo.model.response

import utopia.echo.model.ChatMessage
import utopia.flow.time.Now
import utopia.flow.view.template.Extender

import java.time.Instant

/**
  * A reply from an LLM which has been fully read to memory
  * @param message The reply message itself
  * @param statistics Statistics about the response-generation process
  * @param lastUpdated Origin time of the latest version of message's contents
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
case class BufferedReplyMessage(message: ChatMessage, statistics: ResponseStatistics, lastUpdated: Instant = Now)
	extends Extender[ChatMessage]
{
	override def wrapped: ChatMessage = message
}

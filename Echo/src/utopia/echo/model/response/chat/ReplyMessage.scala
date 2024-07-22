package utopia.echo.model.response.chat

import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.response.OllamaResponse

/**
  * Common trait for chat responses, whether buffered or streamed
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
trait ReplyMessage extends OllamaResponse[BufferedReplyMessage]
{
	/**
	  * @return Role of the sender of this message
	  */
	def senderRole: ChatRole
}
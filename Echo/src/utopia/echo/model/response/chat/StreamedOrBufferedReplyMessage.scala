package utopia.echo.model.response.chat

import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.response.{OllamaResponseWrapper, StreamedOrBufferedResponseFactory}
import utopia.flow.util.EitherExtensions._

object StreamedOrBufferedReplyMessage
	extends StreamedOrBufferedResponseFactory[StreamedReplyMessage, BufferedReplyMessage, StreamedOrBufferedReplyMessage]
{
	override def apply(format: Either[StreamedReplyMessage, BufferedReplyMessage]): StreamedOrBufferedReplyMessage =
		new StreamedOrBufferedReplyMessage(format)
}

/**
  * A wrapper for either a streamed or a buffered chat reply message
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
class StreamedOrBufferedReplyMessage(val format: Either[StreamedReplyMessage, BufferedReplyMessage])
	extends OllamaResponseWrapper[BufferedReplyMessage] with ReplyMessage
{
	// ATTRIBUTES   ----------------------
	
	override protected val wrapped = format.either
	
	
	// IMPLEMENTED  ----------------------
	
	override def senderRole: ChatRole = wrapped.senderRole
}
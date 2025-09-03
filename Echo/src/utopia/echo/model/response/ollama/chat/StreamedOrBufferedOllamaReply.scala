package utopia.echo.model.response.ollama.chat

import utopia.echo.model.enumeration.ChatRole
import utopia.echo.model.response.ollama.{OllamaResponseWrapper, StreamedOrBufferedResponseFactory}
import utopia.flow.util.EitherExtensions._

object StreamedOrBufferedOllamaReply
	extends StreamedOrBufferedResponseFactory[StreamedOllamaReply, BufferedOllamaReply, StreamedOrBufferedOllamaReply]
{
	override def apply(format: Either[StreamedOllamaReply, BufferedOllamaReply]): StreamedOrBufferedOllamaReply =
		new StreamedOrBufferedOllamaReply(format)
}

/**
  * A wrapper for either a streamed or a buffered chat reply message
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
class StreamedOrBufferedOllamaReply(val format: Either[StreamedOllamaReply, BufferedOllamaReply])
	extends OllamaResponseWrapper[BufferedOllamaReply] with OllamaReply
{
	// ATTRIBUTES   ----------------------
	
	override protected val wrapped = format.either
	
	
	// IMPLEMENTED  ----------------------
	
	override def senderRole: ChatRole = wrapped.senderRole
}
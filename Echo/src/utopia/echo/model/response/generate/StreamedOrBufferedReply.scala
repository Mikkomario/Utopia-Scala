package utopia.echo.model.response.generate

import utopia.echo.model.response.{OllamaResponseWrapper, StreamedOrBufferedResponseFactory}
import utopia.flow.collection.CollectionExtensions._

object StreamedOrBufferedReply
	extends StreamedOrBufferedResponseFactory[StreamedReply, BufferedReply, StreamedOrBufferedReply]
{
	override def apply(format: Either[StreamedReply, BufferedReply]): StreamedOrBufferedReply =
		new StreamedOrBufferedReply(format)
}

/**
  * A reply that appears either in a streamed or a buffered format
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  */
class StreamedOrBufferedReply(val format: Either[StreamedReply, BufferedReply])
	extends OllamaResponseWrapper[BufferedReply] with Reply
{
	// ATTRIBUTES   ------------------------
	
	override protected val wrapped = format.either
}

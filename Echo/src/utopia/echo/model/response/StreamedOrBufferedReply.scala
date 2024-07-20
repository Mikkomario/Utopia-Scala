package utopia.echo.model.response

import utopia.flow.collection.CollectionExtensions._

import scala.language.implicitConversions

object StreamedOrBufferedReply
{
	// IMPLICIT ------------------------
	
	implicit def streamed(streamedReply: StreamedReply): StreamedOrBufferedReply =
		new StreamedOrBufferedReply(Left(streamedReply))
		
	implicit def buffered(bufferedReply: BufferedReply): StreamedOrBufferedReply =
		new StreamedOrBufferedReply(Right(bufferedReply))
}

/**
  * A reply that appears either in a streamed or a buffered format
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  */
class StreamedOrBufferedReply(val format: Either[StreamedReply, BufferedReply]) extends ReplyWrapper
{
	// ATTRIBUTES   ------------------------
	
	override protected val wrapped = format.either
}

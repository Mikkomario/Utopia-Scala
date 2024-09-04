package utopia.echo.controller.parser

import utopia.echo.model.response.ResponseStatistics
import utopia.echo.model.response.chat.StreamedReplyMessage
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * Parses streamed chat messages from Ollama API responses.
  * Expects newline-delimited json format.
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
class StreamedReplyMessageResponseParser(implicit override val exc: ExecutionContext,
                                         override val jsonParser: JsonParser, override val log: Logger)
	extends StreamedOllamaResponseParser[StreamedReplyMessage]
{
	override protected def emptyResponse: StreamedReplyMessage = StreamedReplyMessage.empty()
	
	override protected def textFromResponse(response: Model): String = response("message")("content").getString
	
	override protected def responseFrom(textPointer: Changing[String], newTextPointer: Changing[String],
	                                    lastUpdatedPointer: Changing[Instant],
	                                    statisticsFuture: Future[Try[ResponseStatistics]]): StreamedReplyMessage =
		StreamedReplyMessage(textPointer, newTextPointer, lastUpdatedPointer, statisticsFuture)
}

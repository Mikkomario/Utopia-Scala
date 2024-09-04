package utopia.echo.controller.parser

import utopia.echo.model.response.ResponseStatistics
import utopia.echo.model.response.generate.StreamedReply
import utopia.flow.generic.model.immutable.Model
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
  * A response parser which parses LLM replies from a streamed json response.
  * Expects the stream to contain newline-delimited json where each line represents a json object.
  * @param exc Implicit execution context
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
class StreamedReplyResponseParser(implicit override val exc: ExecutionContext, override val jsonParser: JsonParser,
                                  override val log: Logger)
	extends StreamedOllamaResponseParser[StreamedReply]
{
	// IMPLEMENTED  -------------------------
	
	override protected def emptyResponse: StreamedReply = StreamedReply.empty
	
	override protected def textFromResponse(response: Model): String = response("response").getString
	
	override protected def responseFrom(textPointer: Changing[String], newTextPointer: Changing[String],
	                                    lastUpdatedPointer: Changing[Instant],
	                                    statisticsFuture: Future[Try[ResponseStatistics]]): StreamedReply =
		StreamedReply(textPointer, newTextPointer, lastUpdatedPointer, statisticsFuture)
}

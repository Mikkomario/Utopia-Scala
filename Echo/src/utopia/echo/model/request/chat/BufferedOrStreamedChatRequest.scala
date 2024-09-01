package utopia.echo.model.request.chat

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.http.response.ResponseParser
import utopia.echo.controller.EchoContext
import utopia.echo.controller.parser.StreamedReplyMessageResponseParser
import utopia.echo.model.response.chat.{BufferedReplyMessage, StreamedOrBufferedReplyMessage}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

/**
  * Requests an LLM to respond to a chat message.
  * Supports both streamed and buffered reply formats.
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
case class BufferedOrStreamedChatRequest(params: ChatParams, stream: Boolean = false)
                                        (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
	extends ChatRequest[StreamedOrBufferedReplyMessage]
{
	// ATTRIBUTES   --------------------------
	
	private lazy val responseParser = {
		if (stream)
			new StreamedReplyMessageResponseParser().toResponse.mapSuccess(StreamedOrBufferedReplyMessage.streamed)
		else
			ResponseParser.value.tryFlatMapToResponse(EchoContext.parseFailureStatus) {
				_.tryModel.map[StreamedOrBufferedReplyMessage](BufferedReplyMessage.fromOllamaResponse) } {
				_.getString }
	}
	
	
	// IMPLEMENTED  --------------------------
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedOrBufferedReplyMessage]] =
		prepared.send(responseParser)
}

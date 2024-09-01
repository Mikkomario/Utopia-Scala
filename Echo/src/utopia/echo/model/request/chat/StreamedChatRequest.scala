package utopia.echo.model.request.chat

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.controller.parser.StreamedReplyMessageResponseParser
import utopia.echo.model.response.chat.StreamedReplyMessage
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

/**
  * A request where a chat message is sent to an LLM in order to acquire a reply.
  * The replies are read in streamed format, i.e. read / updated word-by-word.
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
case class StreamedChatRequest(params: ChatParams)
                              (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
	extends ChatRequest[StreamedReplyMessage]
{
	// ATTRIBUTES   ------------------------
	
	private lazy val responseParser = new StreamedReplyMessageResponseParser().toResponse
	
	
	// IMPLEMENTED  ------------------------
	
	override def stream: Boolean = true
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedReplyMessage]] =
		prepared.send(responseParser)
}

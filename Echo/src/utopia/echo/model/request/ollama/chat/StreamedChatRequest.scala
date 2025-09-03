package utopia.echo.model.request.ollama.chat

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.controller.parser.StreamedReplyMessageResponseParser
import utopia.echo.model.response.ollama.chat.StreamedOllamaReply
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
	extends ChatRequest[StreamedOllamaReply]
{
	// ATTRIBUTES   ------------------------
	
	private lazy val responseParser = new StreamedReplyMessageResponseParser().toResponse
	
	
	// IMPLEMENTED  ------------------------
	
	override def stream: Boolean = true
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedOllamaReply]] =
		prepared.send(responseParser)
}

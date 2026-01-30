package utopia.echo.model.request.ollama.chat

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.request.ChatParams
import utopia.echo.model.response.ollama.BufferedOllamaReply

import scala.concurrent.Future

/**
  * Requests an LLM to respond to a chat message. Buffers the whole reply message to memory before returning it.
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
case class BufferedChatRequest(params: ChatParams) extends ChatRequest[BufferedOllamaReply]
{
	override def stream: Boolean = false
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedOllamaReply]] =
		prepared.getOne(BufferedOllamaReply.chatResponseParser)
}

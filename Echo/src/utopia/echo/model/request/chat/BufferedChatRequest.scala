package utopia.echo.model.request.chat

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.response.chat.BufferedReplyMessage

import scala.concurrent.Future

/**
  * Requests an LLM to respond to a chat message. Buffers the whole reply message to memory before returning it.
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
case class BufferedChatRequest(params: ChatParams) extends ChatRequest[BufferedReplyMessage]
{
	override def stream: Boolean = false
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedReplyMessage]] =
		prepared.mapModel(BufferedReplyMessage.fromOllamaResponse)
}

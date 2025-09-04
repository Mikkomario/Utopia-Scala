package utopia.echo.model.request.openai

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.request.ChatParams
import utopia.echo.model.response.openai.OpenAiResponse

import scala.concurrent.Future

/**
 * An implementation of [[OpenAiChatRequest]] which doesn't use streaming
 *
 * @author Mikko Hilpinen
 * @since 02.09.2025, v1.4
 */
case class BufferedOpenAiChatRequest(params: ChatParams) extends OpenAiChatRequest[OpenAiResponse]
{
	// ATTRIBUTES   -------------------------
	
	override lazy val stream: Boolean = false
	
	
	// IMPLEMENTED  -------------------------
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[OpenAiResponse]] =
		prepared.getOne(OpenAiResponse)
}
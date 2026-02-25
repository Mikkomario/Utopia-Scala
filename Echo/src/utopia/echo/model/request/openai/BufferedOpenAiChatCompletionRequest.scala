package utopia.echo.model.request.openai

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.request.ChatParams
import utopia.echo.model.response.openai.BufferedOpenAiReply
import utopia.echo.model.settings.ModelSettings
import utopia.flow.generic.model.immutable.Model

import scala.concurrent.Future

object BufferedOpenAiChatCompletionRequest
{
	// OTHER    ---------------------------
	
	/**
	 * @param params Applied parameters
	 * @return A new request
	 */
	def apply(params: ChatParams): BufferedOpenAiChatCompletionRequest =
		_BufferedOpenAiChatCompletionRequest(params)
	
	
	// NESTED   ---------------------------
	
	private case class _BufferedOpenAiChatCompletionRequest(params: ChatParams)
		extends BufferedOpenAiChatCompletionRequest
	{
		override def withSettings(settings: ModelSettings): BufferedOpenAiChatCompletionRequest =
			copy(params = params.withSettings(settings))
		
		override protected def finalizeBody(body: Model): Model = body
		
		override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedOpenAiReply]] =
			prepared.getOne(BufferedOpenAiReply)
	}
}

/**
 * Common trait for buffered requests for Open AI chat completions
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
trait BufferedOpenAiChatCompletionRequest
	extends BufferedOpenAiChatCompletionRequestLike[BufferedOpenAiReply, BufferedOpenAiChatCompletionRequest]

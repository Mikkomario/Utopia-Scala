package utopia.echo.model.request.deepseek

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.openai.{BufferedOpenAiChatCompletionRequest, BufferedOpenAiChatCompletionRequestLike}
import utopia.echo.model.response.openai.BufferedOpenAiReply
import utopia.echo.model.settings.ModelSettings
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}

import scala.concurrent.Future

/**
 * Requests a buffered response for a chat message
 * @author Mikko Hilpinen
 * @since 29.01.2026, v1.5
 */
case class BufferedDeepSeekChatRequest(params: ChatParams)
	extends BufferedOpenAiChatCompletionRequest
		with BufferedOpenAiChatCompletionRequestLike[BufferedOpenAiReply, BufferedDeepSeekChatRequest]
{
	// IMPLEMENTED  ---------------------------
	
	// Specifies thinking mode using DeepSeek-specific syntax
	override protected def finalizeBody(body: Model): Model = params.reasoningEffort match {
		case Some(reasoningEffort) =>
			(body - "reasoning_effort") + Constant("type", if (reasoningEffort.reasons) "enabled" else "disabled")
		case None => body
	}
	
	override def withSettings(settings: ModelSettings): BufferedDeepSeekChatRequest =
		copy(params = params.withSettings(settings))
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedOpenAiReply]] =
		prepared.getOne(BufferedOpenAiReply)
}

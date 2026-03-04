package utopia.echo.model.request.deepseek

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.enumeration.DeepSeekModel.{DeepSeekChat, DeepSeekReasoner}
import utopia.echo.model.enumeration.ReasoningEffort.SkipReasoning
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.openai.{BufferedOpenAiChatCompletionRequest, BufferedOpenAiChatCompletionRequestLike}
import utopia.echo.model.response.openai.BufferedOpenAiReply
import utopia.echo.model.settings.ModelSettings
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}

import scala.concurrent.Future

object BufferedDeepSeekChatRequest
{
	/**
	 * @param params Parameters for constructing this request
	 * @return A new request with the specified parameters
	 */
	def apply(params: ChatParams) = new BufferedDeepSeekChatRequest(params)
}

/**
 * Requests a buffered response for a chat message
 * @author Mikko Hilpinen
 * @since 29.01.2026, v1.5
 */
class BufferedDeepSeekChatRequest(baseParams: ChatParams)
	extends BufferedOpenAiChatCompletionRequest
		with BufferedOpenAiChatCompletionRequestLike[BufferedOpenAiReply, BufferedDeepSeekChatRequest]
{
	// ATTRIBUTES   ---------------------------
	
	override val params: ChatParams = {
		val withCorrectModel = baseParams.reasoningEffort match {
			case Some(SkipReasoning) => baseParams.toLlm(DeepSeekChat)
			case None => baseParams
			case _ => baseParams.toLlm(DeepSeekReasoner)
		}
		withCorrectModel.withReasoningEffort(None)
	}
	
	
	// IMPLEMENTED  ---------------------------
	
	// Specifies thinking mode using DeepSeek-specific syntax
	override protected def finalizeBody(body: Model): Model = baseParams.reasoningEffort match {
		case Some(reasoningEffort) => body + Constant("type", if (reasoningEffort.reasons) "enabled" else "disabled")
		case None => body
	}
	
	override def withSettings(settings: ModelSettings): BufferedDeepSeekChatRequest =
		new BufferedDeepSeekChatRequest(baseParams.withSettings(settings))
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedOpenAiReply]] =
		prepared.getOne(BufferedOpenAiReply)
}

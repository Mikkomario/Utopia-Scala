package utopia.echo.model.request.vllm

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.enumeration.ModelParameter.{MinP, RepeatPenalty, TopK}
import utopia.echo.model.enumeration.ReasoningEffort.SkipReasoning
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.openai.BufferedOpenAiChatCompletionRequest
import utopia.echo.model.response.openai.BufferedOpenAiReply
import utopia.echo.model.settings.ModelSettings
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}

import scala.concurrent.Future

/**
 * A VLLM compatible version of BufferedOpenAiChatCompletionRequest
 * @param baseParams Request parameters
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
// FIXME: Remote reasoning_effort, move extra body to request body
case class BufferedVllmChatCompletionRequest(baseParams: ChatParams)
	extends BufferedOpenAiChatCompletionRequest
{
	// ATTRIBUTES   -----------------------
	
	// vLLM doesn't support reasoning_effort
	override val params: ChatParams = baseParams.withReasoningEffort(None)
	
	
	// IMPLEMENTED  -----------------------
	
	override def withSettings(settings: ModelSettings): BufferedOpenAiChatCompletionRequest =
		BufferedVllmChatCompletionRequest(baseParams.withSettings(settings))
	
	override protected def finalizeBody(body: Model): Model = {
		// Disables thinking for QWEN models with a separate parameter
		val noThink = baseParams.reasoningEffort match {
			case Some(SkipReasoning) => Some(Constant("chat_template_kwargs", Model.from("enable_thinking" -> false)))
			case _ => None
		}
		// TODO: Add "length_penalty", "min_tokens"
		body ++
			Model.from("top_k" -> baseParams.get(TopK), "min_p" -> baseParams.get(MinP),
				"repetition_penalty" -> baseParams.get(RepeatPenalty))
				.withoutEmptyValues ++
			noThink
	}
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedOpenAiReply]] =
		prepared.getOne(BufferedOpenAiReply)
}
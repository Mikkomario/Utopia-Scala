package utopia.echo.model.request.vllm

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.enumeration.ModelParameter.{MinP, RepeatPenalty, TopK}
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.openai.BufferedOpenAiChatCompletionRequest
import utopia.echo.model.response.openai.BufferedOpenAiReply
import utopia.echo.model.settings.ModelSettings
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model}

import scala.concurrent.Future

/**
 * A VLLM compatible version of BufferedOpenAiChatCompletionRequest
 * @param params Request parameters
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
case class BufferedVllmChatCompletionRequest(params: ChatParams)
	extends BufferedOpenAiChatCompletionRequest
{
	override def withSettings(settings: ModelSettings): BufferedOpenAiChatCompletionRequest =
		copy(params = params.withSettings(settings))
	
	override protected def finalizeBody(body: Model): Model = {
		// TODO: Add "length_penalty", "min_tokens"
		Model.from("top_k" -> params(TopK), "min_p" -> params(MinP), "repetition_penalty" -> params(RepeatPenalty))
			.withoutEmptyValues.notEmpty match
		{
			case Some(extraParams) => body + Constant("extra_body", extraParams)
			case None => body
		}
	}
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedOpenAiReply]] =
		prepared.getOne(BufferedOpenAiReply)
}
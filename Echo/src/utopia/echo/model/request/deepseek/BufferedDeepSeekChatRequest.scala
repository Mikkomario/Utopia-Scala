package utopia.echo.model.request.deepseek

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.flow.generic.casting.ValueConversions._
import utopia.disciple.model.request.Body
import utopia.echo.model.enumeration.ModelParameter.{FrequencyPenalty, PredictTokens, PresencePenalty, Stop, Temperature, TopP}
import utopia.echo.model.llm.{HasImmutableModelSettings, ModelSettings}
import utopia.echo.model.request.ChatParams
import utopia.echo.model.response.deepseek.BufferedDeepSeekReply
import utopia.flow.generic.model.immutable.{Model, Value}

import scala.concurrent.Future

/**
 * Requests a buffered response for a chat message
 * @author Mikko Hilpinen
 * @since 29.01.2026, v1.4.1
 */
// TODO: Add support for tool_choice (see: https://api-docs.deepseek.com/api/create-chat-completion)
case class BufferedDeepSeekChatRequest(params: ChatParams)
	extends ApiRequest[BufferedDeepSeekReply] with HasImmutableModelSettings[BufferedDeepSeekChatRequest]
{
	// ATTRIBUTES   ----------------------------
	
	override val method: Method = Post
	override val path: String = "chat/completions"
	override val pathParams: Model = Model.empty
	
	override val body: Either[Value, Body] = {
		val thinking = params.think.exact.map { think => Model.from("type" -> (if (think) "enabled" else "disabled")) }
		Left(Model
			.from(
				"messages" -> params.messages, "model" -> params.llm.llmName, "thinking" -> thinking,
				"frequency_penalty" -> params.get(FrequencyPenalty), "max_tokens" -> params.get(PredictTokens),
				"presence_penalty" -> params.get(PresencePenalty), "stop" -> params.get(Stop), "stream" -> false,
				"temperature" -> params.get(Temperature), "top_p" -> params.get(TopP), "tools" -> params.tools
			)
			.withoutEmptyValues
		)
	}
	
	override val deprecated: Boolean = false
	
	
	// IMPLEMENTED  ---------------------------
	
	override def settings: ModelSettings = params.settings
	override def withSettings(settings: ModelSettings): BufferedDeepSeekChatRequest =
		copy(params = params.withSettings(settings))
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedDeepSeekReply]] =
		prepared.getOne(BufferedDeepSeekReply)
}

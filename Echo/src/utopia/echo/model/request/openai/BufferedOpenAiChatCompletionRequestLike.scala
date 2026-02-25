package utopia.echo.model.request.openai

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.annex.model.request.ApiRequest
import utopia.disciple.model.request.Body
import utopia.echo.model.enumeration.ModelParameter._
import utopia.echo.model.request.ChatParams
import utopia.echo.model.settings.{HasImmutableModelSettings, ModelSettings}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}

/**
 * Common trait for requests for a buffered chat message response using the /chat/completions API endpoint
 * @author Mikko Hilpinen
 * @since 29.01.2026, v1.5
 */
// TODO: Add support for tool_choice (see: https://api-docs.deepseek.com/api/create-chat-completion)
trait BufferedOpenAiChatCompletionRequestLike[+Reply, +Repr]
	extends ApiRequest[Reply] with HasImmutableModelSettings[Repr]
{
	// ABSTRACT --------------------------------
	
	/**
	 * @return Applied request parameters
	 */
	def params: ChatParams
	
	/**
	 * Allows the implementing class to modify the request body before sending it
	 * @param body Body to finalize
	 * @return A finalized version of that body
	 */
	protected def finalizeBody(body: Model): Model
	
	
	// ATTRIBUTES   ----------------------------
	
	override def method: Method = Post
	override def path: String = "chat/completions"
	override def pathParams: Model = Model.empty
	override def deprecated: Boolean = params.deprecationView.value
	
	override def body: Either[Value, Body] = {
		Left(finalizeBody(Model
			.from(
				"messages" -> params.messages, "model" -> params.llm.llmName,
				"reasoning_effort" -> params.reasoningEffort,
				"frequency_penalty" -> params.get(FrequencyPenalty), "max_tokens" -> params.get(PredictTokens),
				"presence_penalty" -> params.get(PresencePenalty), "stop" -> params.get(Stop), "stream" -> false,
				"temperature" -> params.get(Temperature), "top_p" -> params.get(TopP), "tools" -> params.tools
			)
			.withoutEmptyValues)
		)
	}
	
	
	// IMPLEMENTED  ---------------------------
	
	override def settings: ModelSettings = params.settings
}

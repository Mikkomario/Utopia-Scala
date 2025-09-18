package utopia.echo.model.request.openai

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.annex.model.request.ApiRequest
import utopia.disciple.model.request.Body
import utopia.echo.model.enumeration.ModelParameter.{PredictTokens, Temperature, TopP}
import utopia.echo.model.llm.{HasModelSettings, ModelSettings}
import utopia.echo.model.request.ChatParams
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}

/**
  * A request implementation for sending a chat message to the Open AI
  * @author Mikko Hilpinen
  * @since 07.04.2025, v1.3
  */
// TODO: Add support for "metadata": https://platform.openai.com/docs/api-reference/responses/create#responses_create-metadata
// TODO: Add "background" mode support. See: https://platform.openai.com/docs/guides/background
// TODO: When applicable, add "include" property: https://platform.openai.com/docs/api-reference/responses/create#responses_create-include
// TODO: Add reasoning effort configuration: https://platform.openai.com/docs/api-reference/responses/create#responses_create-reasoning
// TODO: Add support for "service_tier": https://platform.openai.com/docs/api-reference/responses/create#responses_create-service_tier
// TODO: May add support for "tool_choice" to force the model to use tools: https://platform.openai.com/docs/api-reference/responses/create#responses_create-tool_choice
// TODO: May add support for truncation: https://platform.openai.com/docs/api-reference/responses/create#responses_create-truncation
trait OpenAiChatRequest[+A] extends ApiRequest[A] with HasModelSettings
{
	// ABSTRACT -------------------------
	
	/**
	  * @return Parameters that specify the details of this request
	  */
	// TODO: Add support for "max_tool_calls" -parameter:
	//  https://platform.openai.com/docs/api-reference/responses/create#responses_create-max_tool_calls
	// TODO: Also add support for "parallel_tool_calls": https://platform.openai.com/docs/api-reference/responses/create#responses_create-parallel_tool_calls
	def params: ChatParams
	/**
	  * @return Whether to request the response as a stream (true) or as a single buffered value (false).
	  */
	def stream: Boolean
	
	
	// IMPLEMENTED  ---------------------
	
	override def method: Method = Post
	override def path: String = "responses"
	override def pathParams: Model = Model.empty
	
	// TODO: Add support for image quality settings and other image settings
	// TODO: Add support for built-in tools
	override def body: Either[Value, Body] = {
		// Converts the messages to objects
		val messages = params.messages.map { message =>
			val content: Value = {
				if (message.encodedImages.isEmpty)
					message.text
				else {
					val builder = OptimizedIndexedSeq.newBuilder[Model]
					if (message.text.nonEmpty)
						builder += Model.from("type" -> "input_text", "text" -> message.text)
					builder ++= message.encodedImages.map { imageData =>
						Model.from("type" -> "input_image", "detail" -> "auto",
							"image_url" -> s"data:image/png;base64,$imageData")
					}
					builder.result()
				}
			}
			Model.from("type" -> "message", "role" -> message.senderRole.name, "content" -> content)
		}
		// Adds parameters for customization, where applicable
		val paramsBuilder = OptimizedIndexedSeq.newBuilder[Constant]
		params(PredictTokens).notEmpty.foreach { maxOutputTokens =>
			paramsBuilder += Constant("max_output_tokens", maxOutputTokens)
		}
		params(Temperature).notEmpty.foreach { temp => paramsBuilder += Constant("temperature", temp) }
		params(TopP).notEmpty.foreach { topP => paramsBuilder += Constant("top_p", topP) }
		
		Left(Model.from("model" -> params.llm.llmName, "input" -> messages,
			"tools" -> params.tools.map { _.toModel + Constant("strict", false) }, "stream" -> stream, "store" -> false) ++
			paramsBuilder.result())
	}
	
	override def settings: ModelSettings = params.settings
	override def deprecated: Boolean = params.deprecationView.value
}

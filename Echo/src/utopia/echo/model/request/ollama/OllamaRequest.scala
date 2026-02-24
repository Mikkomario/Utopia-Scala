package utopia.echo.model.request.ollama

import utopia.access.model.enumeration.Method
import utopia.access.model.enumeration.Method.Post
import utopia.annex.model.request.ApiRequest
import utopia.disciple.model.request.Body
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.settings.HasModelSettings
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.util.UncertainBoolean

import scala.language.implicitConversions

/**
  * Common trait for requests to the Ollama API generation endpoints.
  * These requests are used for sending a prompt and acquiring a reply.
  * This trait isn't fixed to either the streamed or the buffered response format.
  * @tparam R Type of response acquired for this request
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
trait OllamaRequest[+R] extends ApiRequest[R] with HasModelSettings
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Name of the targeted LLM
	  */
	def llm: LlmDesignator
	
	/**
	  * @return Additional properties added to the request body.
	  */
	def customProperties: Seq[Constant]
	
	/**
	  * @return Whether to receive the response as a stream.
	  *         - If false the response will be received only once generation completes,
	  *         and will contain all the information at once.
	  *         - If true, the response text will be received word by word
	  */
	def stream: Boolean
	/**
	 * @return Whether to enable thinking / reflection features for LLMs that support it.
	 *              - true if thinking should be enabled => Yields a separate thinking and text output
	 *              - false if thinking should be disabled => The LLM should not enter thinking mode
	 *              - Uncertain if no adjustment should be made =>
	 *                The LLM may enter thinking mode. Output will be generated as normal text.
	 */
	def think: UncertainBoolean
	
	
	// IMPLEMENTED  ----------------------
	
	override def method: Method = Post
	override def pathParams: Model = Model.empty
	
	override def body: Either[Value, Body] = {
		val baseModel = Model.from(
			"model" -> llm.llmName,
			"stream" -> stream,
			"options" -> optionsModel.notEmpty,
			"think" -> think
		)
		val bodyModel = (baseModel ++ customProperties).withoutEmptyValues
		Left(bodyModel)
	}
	
	
	// OTHER    --------------------------
	
	private def optionsModel = Model
		.withConstants(settings.defined.flatMap { case (param, value) =>
			value.castTo(param.dataType).flatMap { _.notEmpty }.map { Constant(param.key, _) }
		})
		.withoutEmptyValues
}

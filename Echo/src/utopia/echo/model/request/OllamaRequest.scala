package utopia.echo.model.request

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.model.request.ApiRequest
import utopia.disciple.http.request.Body
import utopia.echo.model.LlmDesignator
import utopia.echo.model.enumeration.ModelParameter
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Model, Value}

import scala.language.implicitConversions


/**
  * Common trait for requests to the Ollama API generation endpoints.
  * These requests are used for sending a prompt and acquiring a reply.
  * This trait isn't fixed to either the streamed or the buffered response format.
  * @tparam R Type of response acquired for this request
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
trait OllamaRequest[+R] extends ApiRequest[R]
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
	  * @return Behavioral parameters included in this request.
	  *         Empty values will not be sent.
	  */
	def options: Map[ModelParameter, Value]
	
	/**
	  * @return Whether to receive the response as a stream.
	  *         - If false the response will be received only once generation completes,
	  *         and will contain all the information at once.
	  *         - If true, the response text will be received word by word
	  */
	def stream: Boolean
	
	
	// IMPLEMENTED  ----------------------
	
	override def method: Method = Post
	
	override def body: Either[Value, Body] = {
		val baseModel = Model.from(
			"model" -> llm.name,
			"stream" -> stream,
			"options" -> optionsModel.notEmpty)
		Left((baseModel ++ customProperties).withoutEmptyValues)
	}
	
	
	// OTHER    --------------------------
	
	private def optionsModel = Model
		.withConstants(options.flatMap { case (param, value) =>
			value.castTo(param.dataType).flatMap { _.notEmpty }.map { Constant(param.key, _) }
		})
		.withoutEmptyValues
}
package utopia.echo.model.request.generate

import utopia.echo.model.enumeration.ModelParameter
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.request.OllamaRequest
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

object Generate
{
	// OTHER    -------------------------
	
	/**
	  * @param params Request parameters to apply
	  * @return A factory for finalizing the request by specifying whether it is streamed or buffered
	  */
	def apply(params: GenerateParams) = GenerateRequestFactory(params)
	
	
	// NESTED   -------------------------
	
	/**
	  * Factory used for constructing various types of generate requests
	  * @param params Request parameters to apply
	  */
	case class GenerateRequestFactory(params: GenerateParams)
	{
		// COMPUTED    --------------------------
		
		/**
		  * @return A request for receiving the response as a single entity.
		  */
		def buffered = GenerateBuffered(params)
		/**
		  * @param exc Implicit execution context utilized in streamed response-processing
		  * @param jsonParser Implicit json parser used in response-parsing
		  * @return A request for receiving the response in a streamed format.
		  */
		def streamed(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger) = GenerateStreamed(params)
		/**
		  * @param stream Whether to receive the response as a stream.
		  *               - If false (default) the response will be received only once generation completes,
		  *               and will contain all the information at once.
		  *               - If true, the response text will be received word by word
		  * @param exc Implicit execution context utilized in streamed response-processing
		  * @param jsonParser Implicit json parser used in response-parsing
		  * @return A new request
		  */
		def apply(stream: Boolean = false)(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger) =
			GenerateBufferedOrStreamed(params)
	}
}

/**
  * Common trait for requests to the Ollama API generate endpoint.
  * These requests are used for sending a prompt and acquiring a reply.
  * This trait doesn't define whether the response is read in a streamed or in a buffered format.
  * @tparam R Type of response acquired for this request
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
trait Generate[+R] extends OllamaRequest[R]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Main request parameters
	  */
	def params: GenerateParams
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The query to send to the LLM
	  */
	def query: Query = params.query
	
	
	// IMPLEMENTED  ----------------------
	
	override def path: String = "generate"
	
	override def llm: LlmDesignator = params.llm
	override def options: Map[ModelParameter, Value] = params.options
	override def deprecated: Boolean = params.deprecationView.value
	
	override def customProperties: Seq[Constant] = Vector(
		Constant("prompt", query.toPrompt),
		Constant("format", if (query.expectsJsonResponse) "json" else Value.empty),
		Constant("system", query.toSystem),
		Constant("context", params.conversationContext),
		Constant("images", query.encodedImages)
	)
}

package utopia.echo.model.request.ollama.generate

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.controller.parse.ResponseParser
import utopia.echo.controller.EchoContext
import utopia.echo.controller.parser.StreamedOllamaResponseParser
import utopia.echo.model.llm.{LlmDesignator, ModelSettings}
import utopia.echo.model.request.ollama.OllamaRequest
import utopia.echo.model.response.ollama.{BufferedOllamaReply, OllamaReply}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.UncertainBoolean
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

object GenerateRequest
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
		def buffered = GenerateBufferedRequest(params)
		/**
		  * @param exc Implicit execution context utilized in streamed response-processing
		  * @param jsonParser Implicit json parser used in response-parsing
		  * @return A request for receiving the response in a streamed format.
		  */
		def streamed(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger) =
			apply(stream = true)
		/**
		  * @param stream Whether to receive the response as a stream.
		  *               - If false (default) the response will be received only once generation completes,
		  *               and will contain all the information at once.
		  *               - If true, the response text will be received word by word
		  * @param exc Implicit execution context utilized in streamed response-processing
		  * @param jsonParser Implicit json parser used in response-parsing
		  * @return A new request
		  */
		def apply(stream: Boolean = false)
		         (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger): GenerateRequest[OllamaReply] =
			_GenerateRequest(params, stream)
	}
	
	private case class _GenerateRequest(params: GenerateParams, stream: Boolean = false)
	                                   (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
		extends GenerateRequest[OllamaReply]
	{
		// ATTRIBUTES   ----------------------
		
		private lazy val responseParser: ResponseParser[Response[OllamaReply]] = {
			// Case: Expecting a streamed response => Utilizes StreamedReplyResponseParser
			if (stream)
				StreamedOllamaResponseParser.generate.toResponse
			// Case: Expecting a buffered response => Parses the reply from a response model
			else
				ResponseParser.value.tryFlatMapToResponse(EchoContext.parseFailureStatus) {
					_.tryModel.map { model => BufferedOllamaReply.fromOllamaGenerateResponse(model) } } {
					_.getString }
		}
		
		
		// IMPLEMENTED  ----------------------
		
		override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[OllamaReply]] =
			prepared.send(responseParser)
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
trait GenerateRequest[+R] extends OllamaRequest[R]
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
	override def settings: ModelSettings = params.settings
	override def deprecated: Boolean = params.deprecationView.value
	override def think: UncertainBoolean = params.think
	
	override def customProperties: Seq[Constant] = Vector(
		Constant("prompt", query.toPrompt),
		Constant("format", if (query.expectsJsonResponse) "json" else Value.empty),
		Constant("system", query.toSystem),
		Constant("context", params.conversationContext),
		Constant("images", query.encodedImages)
	)
}

package utopia.echo.model.request.ollama.chat

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.controller.parse.ResponseParser
import utopia.echo.controller.parser.StreamedOllamaResponseParser
import utopia.echo.model.enumeration.ReasoningEffort.SkipReasoning
import utopia.echo.model.llm.LlmDesignator
import utopia.echo.model.settings.ModelSettings
import utopia.echo.model.request.ChatParams
import utopia.echo.model.request.ollama.OllamaRequest
import utopia.echo.model.response.ollama.{BufferedOllamaReply, OllamaReply}
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Constant
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.{NotEmpty, UncertainBoolean}
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

object OllamaChatRequest
{
	// OTHER    -----------------------------
	
	/**
	  * @param params Request parameters to apply
	  * @return A factory for converting the specified parameters into a request
	  */
	def apply(params: ChatParams) = OllamaChatRequestFactory(params)
	
	
	// NESTED   -----------------------------
	
	case class OllamaChatRequestFactory(params: ChatParams)
	{
		// COMPUTED -------------------------
		
		/**
		  * @return A chat request which requests the whole response to be provided at once, without streaming.
		  */
		def buffered = BufferedOllamaChatRequest(params)
		/**
		  * @param exc Implicit execution context
		  * @param jsonParser Implicit json parser
		  * @param log Implicit logging implementation
		  * @return A chat request which requests the response to be streamed word-by-word
		  */
		def streamed(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger) =
			apply(stream = true)
		
		
		// OTHER    -------------------------
		
		/**
		  * @param stream Whether the response should be streamed (true) or buffered and received all at once (false)
		  * @param exc Implicit execution context
		  * @param jsonParser Implicit json parser
		  * @param log Implicit logging implementation
		  * @return A chat request which requests the response to be either streamed (word-by-word) or buffered
		  */
		def apply(stream: Boolean)
		         (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger): OllamaChatRequest[OllamaReply] =
			_StreamedOllamaChatRequest(params, stream)
	}
	
	private case class _StreamedOllamaChatRequest(params: ChatParams, stream: Boolean = false)
	                                             (implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger)
		extends OllamaChatRequest[OllamaReply]
	{
		// ATTRIBUTES   ------------------------
		
		private lazy val responseParser: ResponseParser[Response[OllamaReply]] = {
			if (stream)
				StreamedOllamaResponseParser.chat.toResponse
			else
				ResponseParser.value.tryFlatMapToResponse {
					_.tryModel.flatMap[OllamaReply](BufferedOllamaReply.chatResponseParser.apply) } {
					_.getString }
		}
		
		
		// IMPLEMENTED  ------------------------
		
		override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[OllamaReply]] =
			prepared.send(responseParser)
	}
	
}

/**
  * Common trait for chat requests, which are used for conversing with LLMs
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
trait OllamaChatRequest[+R] extends OllamaRequest[R]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return Request parameters
	  */
	def params: ChatParams
	
	
	// IMPLEMENTED  -------------------------
	
	override def path: String = "chat"
	
	override def llm: LlmDesignator = params.llm
	override def settings: ModelSettings = params.settings
	
	override def deprecated: Boolean = params.deprecationView.value
	override def think: UncertainBoolean = params.reasoningEffort match {
		case Some(SkipReasoning) => false
		case None => UncertainBoolean
		case _ => true
	}
	
	override def customProperties: Seq[Constant] =
		Pair(Constant("messages", params.messages), Constant("tools", NotEmpty(params.tools)))
}

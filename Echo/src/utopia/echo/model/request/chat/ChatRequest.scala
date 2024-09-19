package utopia.echo.model.request.chat

import utopia.echo.model.LlmDesignator
import utopia.echo.model.enumeration.ModelParameter
import utopia.echo.model.request.OllamaRequest
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Constant, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.NotEmpty
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

object ChatRequest
{
	// OTHER    -----------------------------
	
	/**
	  * @param params Request parameters to apply
	  * @return A factory for converting the specified parameters into a request
	  */
	def apply(params: ChatParams) = ChatRequestFactory(params)
	
	
	// NESTED   -----------------------------
	
	case class ChatRequestFactory(params: ChatParams)
	{
		// COMPUTED -------------------------
		
		/**
		  * @return A chat request which requests the whole response to be provided at once, without streaming.
		  */
		def buffered = BufferedChatRequest(params)
		/**
		  * @param exc Implicit execution context
		  * @param jsonParser Implicit json parser
		  * @param log Implicit logging implementation
		  * @return A chat request which requests the response to be streamed word-by-word
		  */
		def streamed(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger) = StreamedChatRequest(params)
		
		
		// OTHER    -------------------------
		
		/**
		  * @param stream Whether the response should be streamed (true) or buffered and received all at once (false)
		  * @param exc Implicit execution context
		  * @param jsonParser Implicit json parser
		  * @param log Implicit logging implementation
		  * @return A chat request which requests the response to be either streamed (word-by-word) or buffered
		  */
		def apply(stream: Boolean)(implicit exc: ExecutionContext, jsonParser: JsonParser, log: Logger) =
			BufferedOrStreamedChatRequest(params, stream)
	}
}

/**
  * Common trait for chat requests, which are used for conversing with LLMs
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
trait ChatRequest[+R] extends OllamaRequest[R]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return Request parameters
	  */
	def params: ChatParams
	
	
	// IMPLEMENTED  -------------------------
	
	override def path: String = "chat"
	
	override def llm: LlmDesignator = params.llm
	override def options: Map[ModelParameter, Value] = params.options
	
	override def deprecated: Boolean = params.deprecationView.value
	
	override def customProperties: Seq[Constant] =
		Pair(Constant("messages", params.messages), Constant("tools", NotEmpty(params.tools)))
}

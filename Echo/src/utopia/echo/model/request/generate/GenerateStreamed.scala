package utopia.echo.model.request.generate

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.controller.parser.StreamedReplyResponseParser
import utopia.echo.model.LlmDesignator
import utopia.echo.model.response.generate.StreamedReply
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

/**
  * A request for the Ollama API to generate a response to a specific query.
  * Processes the reply in a streamed fashion, updating the response text as it arrives.
  * Not suitable for JSON-based requests.
  * @param prompt The prompt to present to the LLM
  * @param conversationContext 'context' property returned by the last LLM response,
  *                            if conversation context should be kept.
  *                            Default = empty = new conversation.
  * @param llm Name of the targeted LLM
  * @param exc Implicit execution context utilized in streamed response-processing
  * @param jsonParser Implicit json parser used in response-parsing
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
class GenerateStreamed(prompt: Prompt, override val conversationContext: Value = Value.empty,
                       testDeprecation: => Boolean = false)
                      (implicit override val llm: LlmDesignator, exc: ExecutionContext, jsonParser: JsonParser,
                       log: Logger)
	extends Generate[StreamedReply]
{
	// ATTRIBUTES   -----------------------------
	
	override lazy val query: Query = Query(prompt)
	private lazy val responseParser = new StreamedReplyResponseParser().toResponse
	
	
	// IMPLEMENTED  -----------------------------
	
	override def stream: Boolean = true
	override def deprecated: Boolean = testDeprecation
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedReply]] =
		prepared.send(responseParser)
}

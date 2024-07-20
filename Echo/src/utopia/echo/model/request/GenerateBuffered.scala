package utopia.echo.model.request

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.RequestResult
import utopia.echo.model.LlmDesignator
import utopia.echo.model.response.BufferedReply
import utopia.flow.generic.model.immutable.Value

import scala.concurrent.Future

/**
  * A request for the Ollama API to generate a reply to a query. Buffers the whole response before returning it.
  * @param query The query to send to the LLM
  * @param conversationContext 'context' property returned by the last LLM response,
  *              if conversation context should be kept.
  *              Default = empty = new conversation.
  * @param testDeprecation A function which yields true if this request gets deprecated and should be retracted
  *              (if not yet sent out).
  *              Default = always false.
  * @param llm Name of the targeted LLM
  * @author Mikko Hilpinen
  * @since 20.07.2024, v1.0
  */
class GenerateBuffered(override val query: Query, override val conversationContext: Value,
                       testDeprecation: => Boolean = false)
                      (implicit override val llm: LlmDesignator)
	extends Generate[BufferedReply]
{
	override def stream: Boolean = false
	override def deprecated: Boolean = testDeprecation
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[BufferedReply]] =
		prepared.mapModel(BufferedReply.fromOllamaResponse)
}

package utopia.echo.model.request

import utopia.annex.controller.ApiClient
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.http.response.ResponseParser
import utopia.echo.controller.{EchoContext, StreamedReplyResponseParser}
import utopia.echo.model.LlmDesignator
import utopia.echo.model.response.{BufferedReply, StreamedOrBufferedReply}
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonParser

import scala.concurrent.{ExecutionContext, Future}

/**
  * A request for the Ollama API to generate a response to a prompt.
  * The response may be received in either streamed or buffered format.
  * @param query The query to send to the LLM
  * @param conversationContext 'context' property returned by the last LLM response,
  *                            if conversation context should be kept.
  *                            Default = empty = new conversation.
  * @param stream Whether to receive the response as a stream.
  *               - If false (default) the response will be received only once generation completes,
  *               and will contain all the information at once.
  *               - If true, the response text will be received word by word
  * @param testDeprecation A function which yields true if this request gets deprecated and should be retracted
  *                        (if not yet sent out).
  *                        Default = always false.
  * @param llm Name of the targeted LLM
  * @param exc Implicit execution context utilized in streamed response-processing
  * @param jsonParser Implicit json parser used in response-parsing
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
class GenerateBufferedOrStreamed(override val query: Query, override val conversationContext: Value = Value.empty,
                                 override val stream: Boolean = false, testDeprecation: => Boolean = false)
                                (implicit override val llm: LlmDesignator, exc: ExecutionContext,
                                        jsonParser: JsonParser)
	extends Generate[StreamedOrBufferedReply]
{
	// ATTRIBUTES   ----------------------
	
	private lazy val responseParser: ResponseParser[Response[StreamedOrBufferedReply]] = {
		// Case: Expecting a streamed response => Utilizes StreamedReplyResponseParser
		if (stream)
			new StreamedReplyResponseParser().toResponse.mapSuccess(StreamedOrBufferedReply.streamed)
		// Case: Expecting a buffered response => Parses the reply from a response model
		else
			ResponseParser.value.tryFlatMapToResponse(EchoContext.parseFailureStatus) {
				_.tryModel.map { model => StreamedOrBufferedReply.buffered(BufferedReply.fromOllamaResponse(model)) } } {
				_.getString }
	}
	
	
	// IMPLEMENTED  ----------------------
	
	override def deprecated: Boolean = testDeprecation
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedOrBufferedReply]] =
		prepared.send(responseParser)
}

package utopia.echo.controller

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.http.request.Body
import utopia.disciple.http.response.ResponseParser
import utopia.echo.model.LlmDesignator
import utopia.echo.model.request.Query
import utopia.echo.model.response.{BufferedReply, StreamedOrBufferedReply}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser

import scala.concurrent.{ExecutionContext, Future}

/**
  * A request for the Ollama API to generate a response to a prompt
  * @param query The query to send to the LLM
  * @param conversationContext 'context' property returned by the last LLM response,
  *                            if conversation context should be kept.
  *                            Default = empty = new conversation.
  * @param stream Whether to receive the response as a stream.
  *               - If false (default) the response will be received only once generation completes,
  *               and will contain all the information at once.
  *               - If true, the response text will be received word by word
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
class GenerateRequest(query: Query, conversationContext: Value = Value.empty, stream: Boolean = false,
                      testDeprecation: => Boolean = false)
                     (implicit llm: LlmDesignator, exc: ExecutionContext, jsonParser: JsonParser)
	extends ApiRequest[StreamedOrBufferedReply]
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
	
	override def method: Method = Post
	override def path: String = "generate"
	
	override def body: Either[Value, Body] = Left(Model.from(
		"model" -> llm.name, "prompt" -> query.toPrompt,
		"format" -> (if (query.expectsJsonResponse) "json" else Value.empty),
		"system" -> query.toSystem, "context" -> conversationContext,
		"stream" -> stream))
	
	override def deprecated: Boolean = testDeprecation
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[StreamedOrBufferedReply]] =
		prepared.send(responseParser)
}

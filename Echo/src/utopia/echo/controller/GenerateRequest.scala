package utopia.echo.controller

import utopia.access.http.Method
import utopia.access.http.Method.Post
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestResult
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.http.request.Body
import utopia.disciple.http.response.ResponseParser
import utopia.echo.model.request.Query
import utopia.echo.model.LlmDesignator
import utopia.echo.model.response.{StreamedOrBufferedReply, StreamedReply}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.{ExecutionContext, Future}

/**
  * A request for the Ollama API to generate a response to a prompt
  * @author Mikko Hilpinen
  * @since 18.07.2024, v1.0
  */
class GenerateRequest(query: Query, conversationContext: Value = Value.empty, stream: Boolean = false,
                      testDeprecation: => Boolean = false)
                     (implicit llm: LlmDesignator, exc: ExecutionContext, jsonParser: JsonParser)
	extends ApiRequest[Changing[StreamedReply]]
{
	// ATTRIBUTES   ----------------------
	
	private lazy val responseParser = {
		if (stream)
			new StreamedReplyResponseParser().toResponse.mapSuccess(StreamedOrBufferedReply.streamed)
		else
			??? // ResponseParser.value.respon
	}
	
	
	// IMPLEMENTED  ----------------------
	
	override def method: Method = Post
	override def path: String = "generate"
	
	// TODO: Test whether json format is even possible / reasonable when streaming
	override def body: Either[Value, Body] = Left(Model.from(
		"model" -> llm.name, "prompt" -> query.toPrompt,
		"format" -> (if (query.expectsJsonResponse) "json" else Value.empty),
		"system" -> query.toSystem, "context" -> conversationContext,
		"stream" -> stream))
	
	override def deprecated: Boolean = testDeprecation
	
	override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Changing[StreamedReply]]] = ???
}

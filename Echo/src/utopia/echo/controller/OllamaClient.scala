package utopia.echo.controller

import utopia.access.http.Status.InternalServerError
import utopia.access.http.{Headers, Status}
import utopia.annex.controller.{ApiClient, PreparingResponseParser}
import utopia.annex.model.response.Response
import utopia.annex.util.ResponseParseExtensions._
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.{Body, StringBody}
import utopia.disciple.http.response.ResponseParser
import utopia.echo.model.LlmDesignator
import utopia.echo.model.request.Query
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/**
  * A client-side interface for interacting with an Ollama API
  * @author Mikko Hilpinen
  * @since 11.07.2024, v0.1
  */
class OllamaClient(serverAddress: String = "http://localhost:11434")
                  (implicit log: Logger, exc: ExecutionContext)
{
	// ATTRIBUTES   ------------------------
	
	// private lazy val queueSystem = new QueueSystem(OllamaApiClient, 5.minutes, minOfflineDelay = 10.seconds)
	
	// private val queues = Cache { _: LlmDesignator => RequestQueue(queueSystem) }
	
	
	// OTHER    ----------------------------
	
	def generate(query: Query, conversationContext: String = "", stream: Boolean = false)
	            (implicit llm: LlmDesignator) =
	{
		val requestBody = Model.from(
			"model" -> llm.name, "prompt" -> query.toPrompt,
			"format" -> (if (query.expectsJsonResponse) "json" else Value.empty),
			"system" -> query.toSystem, "context" -> conversationContext,
			"stream" -> stream)
		// val request = ApiRequest.post("generate", requestBody.withoutEmptyValues)
		
		// val resultFuture = queues(llm).push(request).future
		???
	}
	
	
	// NESTED   ----------------------------
	
	private object OllamaApiClient extends ApiClient
	{
		// ATTRIBUTES   -----------------------
		
		override protected lazy val gateway = Gateway()
		
		override lazy val valueResponseParser: ResponseParser[Response[Value]] =
			ResponseParser.value.unwrapToResponse(responseParseFailureStatus) { _.getString }
		override lazy val emptyResponseParser: ResponseParser[Response[Unit]] =
			PreparingResponseParser.onlyRecordFailures(ResponseParser.stringOrLog)
		
		
		// IMPLEMENTED  -----------------------
		
		override protected implicit def jsonParser: JsonParser = JsonBunny
		override protected implicit def log: Logger = OllamaClient.this.log
		override protected implicit def exc: ExecutionContext = OllamaClient.this.exc
		
		override protected def rootPath: String = serverAddress
		
		override protected def responseParseFailureStatus: Status = EchoContext.parseFailureStatus
		
		override protected def makeRequestBody(bodyContent: Value): Body = StringBody.json(bodyContent.toJson)
		override protected def modifyOutgoingHeaders(original: Headers): Headers = original
	}
}

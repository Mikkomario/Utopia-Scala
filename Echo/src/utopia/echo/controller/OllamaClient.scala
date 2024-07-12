package utopia.echo.controller

import utopia.access.http.Headers
import utopia.annex.controller.{Api, QueueSystem, RequestQueue}
import utopia.annex.model.request.{ApiRequest, GetRequest}
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.{Body, Request, StringBody}
import utopia.echo.model.LlmDesignator
import utopia.echo.model.request.Query
import utopia.flow.collection.immutable.Single
import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

/**
  * A client-side interface for interacting with an Ollama API
  * @author Mikko Hilpinen
  * @since 11.07.2024, v0.1
  */
class OllamaClient(serverAddress: String = "http://localhost:11434")(implicit log: Logger, exc: ExecutionContext)
{
	// ATTRIBUTES   ------------------------
	
	private lazy val queueSystem = new QueueSystem(OllamaApiClient, 5.minutes, minOfflineDelay = 10.seconds)
	
	private val queues = Cache { _: LlmDesignator => RequestQueue(queueSystem) }
	
	
	// OTHER    ----------------------------
	
	def generate(query: Query, conversationContext: String = "", stream: Boolean = false)
	            (implicit llm: LlmDesignator) =
	{
		val requestBody = Model.from(
			"model" -> llm.name, "prompt" -> query.toPrompt,
			"format" -> (if (query.expectsJsonResponse) "json" else Value.empty),
			"system" -> query.toSystem, "context" -> conversationContext,
			"stream" -> stream)
		val request = ApiRequest.post("generate", requestBody.withoutEmptyValues)
		
		val resultFuture = queues(llm).push(request).future
		
	}
	
	
	// NESTED   ----------------------------
	
	private object OllamaApiClient extends Api
	{
		// ATTRIBUTES   -----------------------
		
		override protected lazy val gateway = Gateway(Single(JsonBunny))
		
		
		// IMPLEMENTED  -----------------------
		
		override protected implicit def log: Logger = OllamaClient.this.log
		
		override protected def rootPath: String = serverAddress
		override protected def headers: Headers = Headers.empty
		
		override protected def makeRequestBody(bodyContent: Value): Body = StringBody.json(bodyContent.toJson)
	}
}

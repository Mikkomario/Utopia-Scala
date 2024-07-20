package utopia.echo.controller

import utopia.access.http.{Headers, Status}
import utopia.annex.controller.{ApiClient, PreparingResponseParser, QueueSystem, RequestQueue}
import utopia.annex.model.request.RequestQueueable
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.ResponseParseExtensions._
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.{Body, StringBody}
import utopia.disciple.http.response.ResponseParser
import utopia.flow.async.context.ActionQueue
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

/**
  * A client-side interface for interacting with an Ollama API
  * @author Mikko Hilpinen
  * @since 11.07.2024, v1.0
  */
class OllamaClient(serverAddress: String = "http://localhost:11434")
                  (implicit log: Logger, exc: ExecutionContext)
	extends RequestQueue
{
	// ATTRIBUTES   ------------------------
	
	private lazy val queueSystem = new QueueSystem(OllamaApiClient, 5.minutes, minOfflineDelay = 10.seconds)
	private lazy val queue = RequestQueue(queueSystem)
	
	
	// IMPLEMENTED  ------------------------
	
	override def push[A](request: RequestQueueable[A]): ActionQueue.QueuedAction[RequestResult[A]] = queue.push(request)
	
	
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

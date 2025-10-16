package utopia.echo.controller.client

import utopia.access.model.Headers
import utopia.access.model.enumeration.Status
import utopia.annex.controller.{ApiClient, PreparingResponseParser, QueueSystem, RequestQueue}
import utopia.annex.model.request.RequestQueueable
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.ResponseParseExtensions._
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.controller.Gateway
import utopia.disciple.controller.parse.ResponseParser
import utopia.disciple.model.request.{Body, StringBody}
import utopia.echo.controller.EchoContext
import utopia.flow.async.context.ActionQueue
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext
import utopia.flow.time.Duration

/**
  * A base class for client-side interfaces for interacting with an LLM server
  * @author Mikko Hilpinen
  * @since 29.03.2025, v1.3
  * @param gateway The wrapped gateway instance, which handles http connections, etc.
  * @param serverAddress Address of the server's (root) API path
  * @param apiKey API key sent along with the outgoing requests (default = empty = no API key)
  * @param maxParallelRequests Maximum number of requests that can be active/sent at once
  *                            (default = 1 = parallelism not supported)
  * @param offlineWaitThreshold A request duration after which a connection is considered offline (default = 7 minutes)
  */
class LlmServiceClient(gateway: Gateway, serverAddress: String, apiKey: String = "", maxParallelRequests: Int = 1,
                       offlineWaitThreshold: Duration = 7.minutes)
                      (implicit log: Logger, exc: ExecutionContext)
	extends RequestQueue
{
	/*
		TODO: Add support for DeepSeek, etc.
		 Here are some details for DeepSeek:
		    - Url: https://api.deepseek.com
		    - Model: DeepSeek-V3 | DeepSeek-R1
		    - Needs an API key
	 */
	
	// ATTRIBUTES   ------------------------
	
	/**
	  * The wrapped request queue system
	  */
	protected lazy val queueSystem = new QueueSystem(LlmApiClient, offlineWaitThreshold, minOfflineDelay = 10.seconds)
	/**
	  * The wrapped request queue
	  */
	protected lazy val queue = RequestQueue(queueSystem, maxParallelRequests)
	
	
	// IMPLEMENTED  ------------------------
	
	override def push[A](request: RequestQueueable[A]): ActionQueue.QueuedAction[RequestResult[A]] = queue.push(request)
	
	
	// NESTED   ----------------------------
	
	private object LlmApiClient extends ApiClient
	{
		// ATTRIBUTES   -----------------------
		
		override protected def gateway = LlmServiceClient.this.gateway
		
		override lazy val valueResponseParser: ResponseParser[Response[Value]] =
			ResponseParser.value.unwrapToResponse(responseParseFailureStatus) { v =>
				v("error", "message").stringOr(v.getString)
			}
		override lazy val emptyResponseParser: ResponseParser[Response[Unit]] =
			PreparingResponseParser.onlyRecordFailures(ResponseParser.stringOrLog)
		
		
		// IMPLEMENTED  -----------------------
		
		override protected implicit def jsonParser: JsonParser = JsonBunny
		override protected implicit def log: Logger = LlmServiceClient.this.log
		override protected implicit def exc: ExecutionContext = LlmServiceClient.this.exc
		
		override protected def rootPath: String = serverAddress
		
		override protected def responseParseFailureStatus: Status = EchoContext.parseFailureStatus
		
		override protected def makeRequestBody(bodyContent: Value): Body = StringBody.json(bodyContent.toJson)
		// Adds an API-key automatically, if specified
		override protected def modifyOutgoingHeaders(original: Headers): Headers =
			if (apiKey.isEmpty) original else original.withBearerAuthorization(apiKey)
	}
}

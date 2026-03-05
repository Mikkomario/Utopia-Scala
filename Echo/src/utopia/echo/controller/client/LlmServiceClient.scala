package utopia.echo.controller.client

import utopia.access.model.Headers
import utopia.annex.controller.{ApiClient, QueueSystem, RequestQueue}
import utopia.annex.model.request.RequestQueueable
import utopia.annex.model.response.{RequestResult, Response}
import utopia.bunnymunch.jawn.JsonBunny
import utopia.disciple.controller.parse.ResponseParser
import utopia.disciple.controller.{Gateway, RequestRateLimiter}
import utopia.disciple.model.request.{Body, StringBody}
import utopia.flow.async.context.ActionQueue
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

object LlmServiceClient
{
	/**
	 * Prepares an LLM service client to use with DeepSeek ([[https://api.deepseek.com]])
	 * @param apiKey API key used
	 * @param gateway Gateway instance used. Default = new instance.
	 * @param maxParallelRequests Maximum number of parallel request to send out / process.
	 *                            Default = 8.
	 * @param log Implicit logging implementation
	 * @param exc Implicit execution context
	 * @return a new LLM service client
	 */
	def deepSeek(apiKey: String,
	             gateway: Gateway = Gateway(maxConnectionsPerRoute = 4,
		             allowBodyParameters = false, allowJsonInUriParameters = false),
	             maxParallelRequests: Int = 8)
	            (implicit log: Logger, exc: ExecutionContext) =
		new LlmServiceClient(gateway, "https://api.deepseek.com", apiKey, maxParallelRequests,
			offlineWaitThreshold = 10.minutes)
}

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
                       rateLimiter: Option[RequestRateLimiter] = None, offlineWaitThreshold: Duration = 7.minutes)
                      (implicit log: Logger, exc: ExecutionContext)
	extends RequestQueue
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * The wrapped request queue system
	  */
	protected val queueSystem =
		new QueueSystem(LlmApiClient, offlineWaitThreshold, minOfflineDelay = 10.seconds, maxOfflineDelay = 30.seconds,
			increaseOfflineDelay = _ + 2.5.seconds)
	/**
	  * The wrapped request queue
	  */
	protected val queue = RequestQueue(queueSystem, maxParallelRequests)
	
	
	// IMPLEMENTED  ------------------------
	
	override def push[A](request: RequestQueueable[A]): ActionQueue.QueuedAction[RequestResult[A]] = queue.push(request)
	
	
	// NESTED   ----------------------------
	
	private object LlmApiClient extends ApiClient
	{
		// ATTRIBUTES   -----------------------
		
		override protected implicit val jsonParser: JsonParser = JsonBunny
		
		override val valueResponseParser: ResponseParser[Response[Value]] = newValueResponseParser
		override val emptyResponseParser: ResponseParser[Response[Unit]] = newEmptyResponseParser
		
		
		// IMPLEMENTED  -----------------------
		
		override protected implicit def log: Logger = LlmServiceClient.this.log
		override protected implicit def exc: ExecutionContext = LlmServiceClient.this.exc
		
		override protected def gateway = LlmServiceClient.this.gateway
		override protected def rateLimiter: Option[RequestRateLimiter] = LlmServiceClient.this.rateLimiter
		override protected def rootPath: String = serverAddress
		
		override protected def makeRequestBody(bodyContent: Value): Body = StringBody.json(bodyContent.toJson)
		// Adds an API-key automatically, if specified
		override protected def modifyOutgoingHeaders(original: Headers): Headers =
			if (apiKey.isEmpty) original else original.withBearerAuthorization(apiKey)
	}
}

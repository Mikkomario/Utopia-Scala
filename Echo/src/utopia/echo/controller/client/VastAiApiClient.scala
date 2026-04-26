package utopia.echo.controller.client

import utopia.access.model.Headers
import utopia.annex.controller.ApiClient
import utopia.annex.controller.ApiClient.TooManyRequestsRetrySettings
import utopia.annex.model.response.Response
import utopia.disciple.controller.parse.ResponseParser
import utopia.disciple.controller.{Gateway, RequestRateLimiter}
import utopia.disciple.model.request.{Body, StringBody}
import utopia.flow.async.context.Scheduler
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

/**
 * An API client used for connecting Vast AI
 * @param gateway Utilized [[Gateway]] instance
 * @param apiKey API key used for authorizing requests
 * @param apiVersion Used API version. Default = v0.
 * @author Mikko Hilpinen
 * @since 25.02.2026, v1.5
 */
class VastAiApiClient(override protected val gateway: Gateway, apiKey: String, apiVersion: Int = 0)
                     (implicit override protected val exc: ExecutionContext,
                      override protected val scheduler: Scheduler, override protected val log: Logger,
                      override protected val jsonParser: JsonParser)
	extends ApiClient
{
	// ATTRIBUTES   ----------------------
	
	override protected val rootPath: String = s"https://console.vast.ai/api/v$apiVersion"
	
	override val valueResponseParser: ResponseParser[Response[Value]] = newValueResponseParser
	override val emptyResponseParser: ResponseParser[Response[Unit]] = newEmptyResponseParser
	
	// API requests too frequent: endpoint threshold=4.5*1.0
	override protected val rateLimiter: Option[RequestRateLimiter] = Some(RequestRateLimiter(1, 1.seconds))
	override protected val tooManyRequestsRetrySettings: Option[ApiClient.TooManyRequestsRetrySettings] =
		Some(TooManyRequestsRetrySettings(appliedRetryAfterRange = Span(1.seconds, 2.minutes)))
	
	
	// IMPLEMENTED  ----------------------
	
	override protected def modifyOutgoingHeaders(original: Headers): Headers = original.withBearerAuthorization(apiKey)
	override protected def makeRequestBody(bodyContent: Value): Body = StringBody.json(bodyContent.toJson)
}

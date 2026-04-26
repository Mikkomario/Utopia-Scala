package utopia.annex.test

import utopia.access.model.Headers
import utopia.annex.controller.ApiClient
import utopia.annex.model.request.GetRequest
import utopia.annex.model.response.{RequestResult, Response}
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.controller.parse.ResponseParser
import utopia.disciple.controller.{Gateway, RequestRateLimiter}
import utopia.disciple.model.request.{Body, StringBody}
import utopia.flow.async.context.Scheduler
import utopia.flow.async.process.Wait
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.test.TestContext
import utopia.flow.test.TestContext._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import scala.concurrent.{ExecutionContext, Future}

/**
 * Tests request rate -limiting in practice
 * @author Mikko Hilpinen
 * @since 14.04.2026, v1.12.1
 */
object RequestRateLimitTest extends App
{
	// ATTRIBUTES   -------------------
	
	private val resultFutures = (1 to 5).map { _ =>
		Wait(0.1.seconds)
		Client.send(TestRequest)
	}
	
	
	// TESTS    -----------------------
	
	Wait(2.seconds)
	
	assert(resultFutures.exists { _.isCompleted })
	assert(resultFutures.exists { !_.isCompleted })
	assert(resultFutures.count { _.isCompleted } == 3, resultFutures.count { _.isCompleted })
	
	Wait(2.0.seconds)
	
	assert(resultFutures.forall { _.isCompleted })
	
	println("Success!")
	
	
	// NESTED   -----------------------
	
	private object TestRequest extends GetRequest[Unit]
	{
		override val path: String = "test"
		override val pathParams: Model = Model.empty
		override val deprecated: Boolean = false
		
		override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[Unit]] = prepared.send()
	}
	
	private object Client extends ApiClient
	{
		override protected implicit val exc: ExecutionContext = TestContext.exc
		override protected implicit val scheduler: Scheduler = TestContext.scheduler
		override protected implicit val log: Logger = TestContext.log
		override protected implicit val jsonParser: JsonParser = JsonReader
		
		override protected val gateway: Gateway = Gateway()
		override protected val rateLimiter: Option[RequestRateLimiter] = Some(RequestRateLimiter(1, 1.seconds))
		override protected val tooManyRequestsRetrySettings: Option[ApiClient.TooManyRequestsRetrySettings] = None
		
		override protected val rootPath: String = "http://test"
		
		override val valueResponseParser: ResponseParser[Response[Value]] =
			ResponseParser.valueOrLog.toResponse { _.getString }
		override val emptyResponseParser: ResponseParser[Response[Unit]] = ResponseParser.empty.toResponse { _ => "" }
		
		override protected def modifyOutgoingHeaders(original: Headers): Headers = original
		
		override protected def makeRequestBody(bodyContent: Value): Body = StringBody.json(bodyContent.toJson)
	}
}

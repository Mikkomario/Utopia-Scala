package utopia.annex.test

import utopia.access.model.Headers
import utopia.access.model.enumeration.ContentCategory.Application
import utopia.annex.controller.{ApiClient, PreparingResponseParser}
import utopia.annex.model.response.Response
import utopia.annex.util.ResponseParseExtensions._
import utopia.disciple.controller.{Gateway, RequestRateLimiter}
import utopia.disciple.controller.parse.ResponseParser
import utopia.disciple.model.request.{Body, StringBody}
import utopia.flow.collection.immutable.Single
import utopia.flow.generic.model.immutable.Value
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

object TestApiClient
{
	def apply(rootPath: String = "http://localhost:9999/test/api/v1")
	         (implicit exc: ExecutionContext, log: Logger, jsonParser: JsonParser) =
		new TestApiClient(exc, jsonParser, log, rootPath)
}

/**
  * A client interface for the Nexus Test Server
  * @author Mikko Hilpinen
  * @since 17.07.2024, v1.8
  */
class TestApiClient(executionContext: ExecutionContext, jsonParseLogic: JsonParser = JsonReader,
                    logger: Logger = SysErrLogger, override val rootPath: String = "http://localhost:9999/test/api/v1")
	extends ApiClient
{
	// ATTRIBUTES   ----------------------
	
	override protected lazy val gateway: Gateway =
		new Gateway(allowBodyParameters = false, allowJsonInUriParameters = true)
	override protected val rateLimiter: Option[RequestRateLimiter] = None
	
	override lazy val valueResponseParser: ResponseParser[Response[Value]] =
		ResponseParser.value.unwrapToResponse { v =>
			v("error", "message", "description", "details").stringOr(v.getString)
		}
	override lazy val emptyResponseParser: ResponseParser[Response[Unit]] =
		PreparingResponseParser.onlyRecordFailures(ResponseParser.value.map {
			case Success(v) => v("error", "message", "description", "details").stringOr(v.getString)
			case Failure(error) =>
				val msg = "Failed to parse the response body"
				logger(error, "Failed to parse the response body")
				msg
		})
	
	
	// IMPLEMENTED  ----------------------
	
	override protected implicit def exc: ExecutionContext = executionContext
	override protected implicit def log: Logger = logger
	override protected implicit def jsonParser: JsonParser = jsonParseLogic
	
	override protected def modifyOutgoingHeaders(original: Headers): Headers = original.mapAcceptedTypes { accepted =>
		if (accepted.isEmpty) Single(Application.json) else accepted
	}
	
	override protected def makeRequestBody(bodyContent: Value): Body = StringBody.json(bodyContent.toJson)
}

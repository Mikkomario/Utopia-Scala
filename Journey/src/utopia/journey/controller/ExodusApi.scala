package utopia.journey.controller

import utopia.access.http.Status.Unauthorized
import utopia.access.http.{Headers, Method}
import utopia.annex.controller.Api
import utopia.annex.model.error.UnauthorizedRequestException
import utopia.annex.model.response.{RequestFailure, RequestResult, Response}
import utopia.disciple.apache.Gateway
import utopia.disciple.http.request.StringBody
import utopia.disciple.model.error.RequestFailedException
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.async.VolatileOption
import utopia.flow.util.StringExtensions._
import utopia.journey.model.UserCredentials

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * An interface used for accessing the Exodus API
  * @author Mikko Hilpinen
  * @since 20.6.2020, v0.1
  */
// Credentials is either basic credentials (left) or a device key (right)
class ExodusApi(override protected val gateway: Gateway = new Gateway(), override val rootPath: String,
                credentials: Either[UserCredentials, String], initialSessionKey: String)
               (implicit override val jsonParser: JsonParser)
	extends Api
{
	// ATTRIBUTES	---------------------------
	
	private var resetSessionThreshold = Now + 18.hours
	private var sessionKey = initialSessionKey
	private val sessionResetFuture = VolatileOption[Future[Try[String]]]()
	
	
	// IMPLEMENTED	---------------------------
	
	override protected implicit def log: Logger = SysErrLogger
	
	override protected def headers = Headers.currentDateHeaders.withBearerAuthorization(sessionKey)
	
	override protected def makeRequestBody(bodyContent: Value) = StringBody.json(bodyContent.toJson)
	
	override protected def makeRequest(method: Method, path: String, timeout: Duration = Duration.Inf,
									   body: Value = Value.empty, params: Model = Model.empty,
									   modHeaders: Headers => Headers = h => h)
									  (implicit context: ExecutionContext): Future[RequestResult] =
	{
		???
		/*
		// May acquire a new session key before making further requests
		if (Now > resetSessionThreshold) {
			LocalDevice.id match {
				case Some(deviceId) =>
					sessionResetFuture.setOneIfEmpty {
						val result = resetSession(deviceId)
						result.onComplete { _ => sessionResetFuture.clear() }
						result
					}.flatMap {
						case Success(newKey) =>
							super.makeRequest(method, path, timeout, body, params,
								h => modHeaders(h.withBearerAuthorization(newKey)))
						case Failure(error) => Future.successful(RequestSendingFailed(error))
					}
				case None =>
					Future.successful(RequestSendingFailed(
						new RequestFailedException("Device id not known, can't reacquire session key")))
			}
		}
		else
			super.makeRequest(method, path, timeout, body, params, modHeaders)
			
		 */
	}
	
	
	// OTHER	------------------------------
	
	private def resetSession(deviceId: Int)(implicit exc: ExecutionContext) =
	{
		get(s"devices/$deviceId/session-key", headersMod = resetSessionHeadersMod).map {
			case Response.Success(status, responseBody, _) =>
				// Reads the new session key from the response body and uses that from this point onwards
				responseBody.value.string match {
					case Some(newKey) =>
						sessionKey = newKey
						resetSessionThreshold = Now + 18.hours
						Success(sessionKey)
					case None => Failure(new RequestFailedException(
						s"No new session key received in authorization response body ($status)"))
				}
			case Response.Failure(status, message, _) =>
				if (status == Unauthorized)
					Failure(new UnauthorizedRequestException(
						"Couldn't acquire a new session key with the old credentials"))
				else
					Failure(new RequestFailedException(message.nonEmptyOrElse(
						s"Couldn't acquire a new session key. Response status: $status")))
			case failure: RequestFailure => Failure(new RequestFailedException("Couldn't access the server", failure.cause))
		}
	}
	
	private def resetSessionHeadersMod(headers: Headers) = credentials match
	{
		case Right(deviceKey) => headers.withBearerAuthorization(deviceKey)
		case Left(basic) => headers.withBasicAuthorization(basic.email, basic.password)
	}
}

package utopia.journey.controller

import utopia.access.http.Headers
import utopia.access.http.Status.Unauthorized
import utopia.annex.controller.Api
import utopia.annex.model.response.Response
import utopia.annex.model.response.ResponseBody.{Content, Empty}
import utopia.disciple.http.request.StringBody
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.flow.async.AsyncExtensions._
import utopia.journey.model.UserCredentials
import utopia.annex.model.error.{EmptyResponseException, RequestFailedException, UnauthorizedRequestException}
import utopia.metropolis.model.combined.device.FullDevice
import utopia.metropolis.model.combined.user.UserCreationResult
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.post.{NewDevice, NewLanguageProficiency, NewUser}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * This API is used before authorization (session key) is acquired
  * @author Mikko Hilpinen
  * @since 21.6.2020, v1
  */
class UnauthorizedExodusApi(override val rootPath: String) extends Api
{
	// IMPLEMENTED	-----------------------------
	
	override protected def headers = Headers.currentDateHeaders
	
	override protected def makeRequestBody(bodyContent: Value) = StringBody.json(bodyContent.toJson)
	
	
	// OTHER	---------------------------------
	
	/**
	  * Creates a new user and new device on the server side, then logs in as that user
	  * @param userName Name of the new user
	  * @param credentials Credentials for the new user
	  * @param languages Language skills of the new user
	  * @param device Data for creating a new device
	  * @param exc Implicit execution context
	  * @return Access to an authorized API once it becomes available. May result in a failure also.
	  */
	def createNewUserAndDevice(userName: String, credentials: UserCredentials,
							   languages: Vector[NewLanguageProficiency], device: NewDevice)
							  (implicit exc: ExecutionContext) =
	{
		implicit val userParser: UserCreationResult.type = UserCreationResult
		
		// TODO: Add support for email validation (current implementation expects email validation to not be used)
		val newUser = NewUser(Right(UserSettingsData(userName, credentials.email)), credentials.password, languages,
			Left(device), credentials.allowDeviceKeyUse)
		
		// Posts new user data to the server
		post("users", newUser.toModel).tryMapIfSuccess
		{
			// Checks response status and parses user data from the body
			case Response.Success(status, body) =>
				body match
				{
					case c: Content =>
						c.single.parsed.map { user =>
							// TODO: Cache user information
							// Stores received device id, possible device key and session key
							LocalDevice.preInitialize(user.deviceId, device.name, user.userId)
							user.deviceKey.foreach { LocalDevice.key = _ }
							val apiCredentials = user.deviceKey match
							{
								case Some(deviceKey) => Right(deviceKey)
								case None => Left(credentials)
							}
							new ExodusApi(rootPath, apiCredentials, user.sessionKey)
						}
					case Empty => Failure(new EmptyResponseException(
						s"Expected to receive new user data. Instead received an empty response with status $status"))
				}
			case Response.Failure(status, message) =>
				Failure(new RequestFailedException(message.getOrElse(s"Received $status when posting new user")))
		}
	}
	
	/**
	  * Creates a new device on server side and logs in with it. Remember to check unauthorized case.
	  * @param credentials User credentials
	  * @param device New device data
	  * @param exc Implicit execution context
	  * @return Access to an authorized api once it becomes available. May result in a failure.
	  */
	def loginWithNewDevice(credentials: UserCredentials, device: NewDevice)(implicit exc: ExecutionContext) =
	{
		implicit val parser: FullDevice.type = FullDevice
		
		post("devices", device.toModel,
			headersMod = _.withBasicAuthorization(credentials.email, credentials.password)).tryFlatMapIfSuccess {
			case Response.Success(status, body) =>
				body match
				{
					case c: Content =>
						c.single.parsed.map { device =>
							// Stores new device data
							LocalDevice.initialize(device)
							// Logs in with the new device
							if (credentials.allowDeviceKeyUse)
								retrieveAndUseDeviceKey(device.id, credentials)
							else
								login(Left(credentials), device.id)
						} match
						{
							case Success(future) => future
							case Failure(e) => asyncFailure(e)
						}
					case Empty => asyncFailure(new EmptyResponseException(
						s"Expected to receive device data. Instead received an empty response with status $status"))
				}
			case f: Response.Failure => Future.successful(handleLoginFailureResponse(f))
		}
	}
	
	/**
	  * Attempts to log in using a device authorization key. Remember to check for authorization failures.
	  * @param deviceId Id of targeted device (see LocalDevice)
	  * @param deviceKey Authorization key for targeted device (see LocalDevice)
	  * @param exc Implicit execution context
	  * @return Access to authorized api once it becomes available. May contain a failure.
	  */
	def loginWithDeviceKey(deviceId: Int, deviceKey: String)(implicit exc: ExecutionContext) =
		login(Right(deviceKey), deviceId)
	
	/**
	  * Logs in using specified credentials. Remember to check for authorization failures.
	  * @param deviceId Id of targeted device
	  * @param credentials User credentials
	  * @param exc Implicit execution context
	  * @return Access to authorized api once it becomes available. May contain a failure.
	  */
	def login(deviceId: Int, credentials: UserCredentials)(implicit exc: ExecutionContext): Future[Try[ExodusApi]] =
	{
		if (credentials.allowDeviceKeyUse)
			retrieveAndUseDeviceKey(deviceId, credentials)
		else
			login(Left(credentials), deviceId)
	}
	
	private def retrieveAndUseDeviceKey(deviceId: Int, credentials: UserCredentials)(implicit exc: ExecutionContext) =
	{
		get(s"devices/$deviceId/device-key",
			headersMod = _.withBasicAuthorization(credentials.email, credentials.password)).tryFlatMapIfSuccess {
			case Response.Success(status, body) =>
				body.value.string match
				{
					case Some(key) =>
						// Registers the key, then uses it to acquire a session key
						LocalDevice.key = key
						login(Right(key), deviceId)
					case None => asyncFailure(new EmptyResponseException(
						s"Expected a device key but an empty response ($status) received instead"))
				}
			case failure: Response.Failure => Future.successful(handleLoginFailureResponse(failure))
		}
	}
	
	private def login(credentials: Either[UserCredentials, String], deviceId: Int)(implicit exc: ExecutionContext) =
	{
		def modHeaders(headers: Headers) = credentials match
		{
			case Right(deviceKey) => headers.withBearerAuthorization(deviceKey)
			case Left(basic) => headers.withBasicAuthorization(basic.email, basic.password)
		}
		
		get(s"devices/$deviceId/session-key", headersMod = modHeaders).tryMapIfSuccess {
			case Response.Success(status, body) =>
				body.value.string match
				{
					case Some(key) => Success(new ExodusApi(rootPath, credentials, key))
					case None => Failure(new EmptyResponseException(
						s"Expected a session key but received an empty response with status $status"))
				}
			case failure: Response.Failure => handleLoginFailureResponse(failure)
		}
	}
	
	private def handleLoginFailureResponse(response: Response.Failure) =
	{
		if (response.status == Unauthorized)
			Failure(new UnauthorizedRequestException(response.message.getOrElse("Invalid user credentials")))
		else
			Failure(new RequestFailedException(
				response.message.getOrElse(s"Unexpected response status (${response.status})")))
	}
}

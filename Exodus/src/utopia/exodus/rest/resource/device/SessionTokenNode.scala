package utopia.exodus.rest.resource.device

import utopia.access.http.Method.{Delete, Get}
import utopia.access.http.Status.NotFound
import utopia.citadel.database.access.single.device.DbClientDevice
import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.database.access.single.auth.DbSessionToken
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing temporary session-keys, which are used for authorizing requests
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  */
case class SessionTokenNode(deviceId: Int) extends LeafResource[AuthorizedContext]
{
	override val name = "session-token"
	override val allowedMethods = Vector(Get, Delete)
	
	private lazy val deviceAccess = DbClientDevice(deviceId)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// GET retrieves a new temporary session key,
		// invalidating any existing session keys for the user device -combination
		if (context.request.method == Get)
		{
			// Authorizes the request using either device key or basic authorization
			context.basicOrDeviceTokenAuthorized(deviceId) { (userId, deviceKeyWasUsed, connection) =>
				implicit val c: Connection = connection
				// On basic auth mode, makes sure the targeted device exists
				if (deviceKeyWasUsed || deviceAccess.nonEmpty)
				{
					// If basic auth was used, may register a new user device -connection
					if (!deviceKeyWasUsed)
						DbUser(userId).linkToDeviceWithId(deviceId).createIfEmpty()
					val newSession = DbSessionToken.forDeviceSession(userId, deviceId).start(context.modelStyle)
					// Returns the session token
					Result.Success(newSession.token)
				}
				else
					Result.Failure(NotFound, s"There doesn't exist a device with id $deviceId")
			}
		}
		// DELETE invalidates the session token on this device (authorized with a session token)
		else
		{
			context.sessionTokenAuthorized { (session, connection) =>
				DbSessionToken.forDeviceSession(session.userId, deviceId).logOut()(connection)
				Result.Empty
			}
		}
	}
}

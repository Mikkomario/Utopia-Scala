package utopia.exodus.rest.resource.device

import utopia.access.http.Method.{Delete, Get}
import utopia.access.http.Status.NotFound
import utopia.citadel.database.access.single.device.DbClientDevice
import utopia.citadel.database.access.single.user.DbUser
import utopia.exodus.database.access.single.auth.DbDeviceToken
import utopia.exodus.rest.util.AuthorizedContext
import utopia.exodus.util.ExodusContext.uuidGenerator
import utopia.flow.generic.casting.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.nexus.result.Result.Empty
import utopia.vault.database.Connection

/**
  * This resource is used for acquiring long-term device authorization keys
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  */
@deprecated("This rest tree will be removed in a future release", "v4.0")
case class DeviceTokenNode(deviceId: Int) extends LeafResource[AuthorizedContext]
{
	// ATTRIBUTES   -------------------------------
	
	override val name = "device-token"
	override val allowedMethods = Vector(Get, Delete)
	
	private lazy val deviceAccess = DbClientDevice(deviceId)
	
	
	// IMPLEMENTED	-------------------------------
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// On GET, generates a new device token for the authorized (basic auth) user,
		// releasing the token from other device users
		if (context.request.method == Get)
			context.basicAuthorized { (userId, connection) =>
				implicit val c: Connection = connection
				// Makes sure this device exists
				if (deviceAccess.nonEmpty)
				{
					// Registers a new connection between the user and this device, if there wasn't one already
					DbUser(userId).linkToDeviceWithId(deviceId).createIfEmpty()
					// Gets and returns the new device authentication token
					val token = DbDeviceToken.forDeviceWithId(deviceId).assignToUserWithId(userId).token
					Result.Success(token)
				}
				else
					Result.Failure(NotFound, s"There exists no device with id $deviceId")
			}
		// On DELETE, deprecates the authorized user's (session auth) device key for THIS device
		else
		{
			context.sessionTokenAuthorized { (session, connection) =>
				implicit val c: Connection = connection
				// Doesn't target the device associated with the session but with this node
				DbDeviceToken.forDeviceWithId(deviceId).releaseFromUserWithId(session.userId)
				Empty
			}
		}
	}
}

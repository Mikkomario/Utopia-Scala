package utopia.exodus.rest.resource.device

import utopia.access.http.Method.Post
import utopia.exodus.database.access.many.DbDevices
import utopia.exodus.database.access.single.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.device.FullDevice
import utopia.metropolis.model.post.NewDevice
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing device-related information
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  */
object DevicesNode extends Resource[AuthorizedContext]
{
	override val name = "devices"
	
	override val allowedMethods = Vector(Post)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.basicAuthorized { (userId, connection) =>
			context.handlePost(NewDevice) { newDevice =>
				implicit val c: Connection = connection
				// Inserts a new device and links it with the authorized user
				val descriptionLink = DbDevices.insert(newDevice.name, newDevice.languageId, userId)
				DbUser(userId).linkWithDeviceWithId(descriptionLink.targetId)
				
				// Returns a "full" device model
				Result.Success(FullDevice(descriptionLink.targetId, Set(descriptionLink), Set(userId)).toModel)
			}
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		// Allows access to specific devices with device ids
		path.head.int match
		{
			case Some(deviceId: Int) => Follow(DeviceNode(deviceId), path.tail)
			case None => Error(message = Some(s"${path.head} is not a valid device id"))
		}
	}
}

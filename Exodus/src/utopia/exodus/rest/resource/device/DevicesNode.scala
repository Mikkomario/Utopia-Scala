package utopia.exodus.rest.resource.device

import utopia.access.http.Method.Post
import utopia.access.http.Status.BadRequest
import utopia.citadel.database.access.single.device.DbClientDevice
import utopia.citadel.database.access.single.language.DbLanguage
import utopia.citadel.database.model.device.ClientDeviceUserModel
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.device.ClientDeviceUserData
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
		context.basicAuthorized { (userId, connection) =>
			context.handlePost(NewDevice) { newDevice =>
				implicit val c: Connection = connection
				// Makes sure the referred language id is valid
				if (DbLanguage(newDevice.languageId).nonEmpty)
				{
					// Inserts a new device and links it with the authorized user
					val device = DbClientDevice.insert(newDevice, userId)
					val userLink = ClientDeviceUserModel.insert(ClientDeviceUserData(device.id, userId))
					
					// Returns a "detailed" device model
					Result.Success(device.witherUserLinks(Set(userLink)).toModel)
				}
				else
					Result.Failure(BadRequest, s"Referred language id ${newDevice.languageId} is not valid")
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

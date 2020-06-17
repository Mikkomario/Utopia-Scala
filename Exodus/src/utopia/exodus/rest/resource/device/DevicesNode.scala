package utopia.exodus.rest.resource.device

import utopia.access.http.Status.NotImplemented
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result

/**
  * Used for accessing device-related information
  * @author Mikko Hilpinen
  * @since 3.5.2020, v2
  */
object DevicesNode extends Resource[AuthorizedContext]
{
	override val name = "devices"
	
	override val allowedMethods = Vector()
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
		Result.Failure(NotImplemented).toResponse
	
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

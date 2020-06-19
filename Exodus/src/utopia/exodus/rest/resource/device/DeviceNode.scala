package utopia.exodus.rest.resource.device

import utopia.access.http.Status.NotImplemented
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.util.StringExtensions._
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result

/**
  * Used for accessing an individual device's information
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  */
case class DeviceNode(deviceId: Int) extends Resource[AuthorizedContext]
{
	override def name = deviceId.toString
	
	override def allowedMethods = Vector()
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
		Result.Failure(NotImplemented).toResponse
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		// Contains 'device-key' child node which allows access to long-term device specific authorization keys
		if (path.head ~== "device-key")
			Follow(DeviceKeyNode(deviceId), path.tail)
		// 'session-key' child node, on the other hand, provides access to short-term device specific user session keys
		else if (path.head ~== "session-key")
			Follow(SessionKeyNode(deviceId), path.tail)
		else
			Error(message = Some("Device only contains 'device-key' and 'session-key' child nodes"))
	}
}

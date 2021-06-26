package utopia.exodus.rest.resource.device

import utopia.access.http.Method.Get
import utopia.access.http.Status.Unauthorized
import utopia.citadel.database.access.single.DbDevice
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.combined.device.FullDevice
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing an individual device's information
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  */
case class DeviceNode(deviceId: Int) extends Resource[AuthorizedContext]
{
	// IMPLEMENTED	-----------------------------
	
	override def name = deviceId.toString
	
	override def allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Client user must be authorized and be a registered user of this device
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			val deviceAccess = DbDevice(deviceId)
			if (deviceAccess.isUsedByUserWithId(session.userId))
			{
				// May return no content if if-modified-since header is provided and data hasn't been changed
				if (context.request.headers.ifModifiedSince.forall { threshold => deviceAccess.isModifiedSince(threshold) })
				{
					val languageIds = context.languageIdListFor(session.userId)
					val userIds = deviceAccess.userIds
					val descriptions = deviceAccess.descriptions.inLanguages(languageIds)
					Result.Success(FullDevice(deviceId, descriptions.toSet, userIds.toSet).toModel)
				}
				else
					Result.NotModified
			}
			else
				Result.Failure(Unauthorized, "This is not a device you're currently using.")
		}
	}
	
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

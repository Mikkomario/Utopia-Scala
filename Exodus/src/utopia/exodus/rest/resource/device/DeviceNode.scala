package utopia.exodus.rest.resource.device

import utopia.access.http.Method.Get
import utopia.access.http.Status.{NotFound, Unauthorized}
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.single.device.DbClientDevice
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceWithChildren
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing an individual device's information
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  */
case class DeviceNode(deviceId: Int) extends ResourceWithChildren[AuthorizedContext]
{
	// COMPUTED ---------------------------------
	
	private def access = DbClientDevice(deviceId)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def name = deviceId.toString
	
	override def allowedMethods = Vector(Get)
	
	override def children = Vector(DeviceKeyNode(deviceId), SessionKeyNode(deviceId))
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// Client user must be authorized and be a registered user of this device
		context.sessionTokenAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			if (access.isUsedByUserWithId(session.userId))
			{
				// May return no content if if-modified-since header is provided and data hasn't been changed
				if (context.request.headers.ifModifiedSince.forall { threshold => access.isModifiedSince(threshold) })
				{
					implicit lazy val languageIds: LanguageIds = session.languageIds
					access.detailed match
					{
						case Some(device) =>
							Result.Success(session.modelStyle match {
								case Full => device.toModel
								case Simple => device.toSimpleModelUsing(DbDescriptionRoles.pull)
							})
						case None => Result.Failure(NotFound, s"Device id $deviceId is not valid")
					}
				}
				else
					Result.NotModified
			}
			else
				Result.Failure(Unauthorized, "This is not a device you're currently using.")
		}
	}
}

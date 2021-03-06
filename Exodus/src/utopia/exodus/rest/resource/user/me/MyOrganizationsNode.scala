package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.Get
import utopia.citadel.database.access.many.description.DbDescriptionRoles
import utopia.citadel.database.access.single.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.ModelStyle.{Full, Simple}
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceWithChildren
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * This rest node returns a descriptive list of all the organizations the authorized user belongs to
 * @author Mikko Hilpinen
 * @since 6.5.2020, v1
 */
object MyOrganizationsNode extends ResourceWithChildren[AuthorizedContext]
{
	// IMPLEMENTED	------------------------
	
	override val name = "organizations"
	
	override val allowedMethods = Vector(Get)
	
	override def children = Vector(DeletionsForMyOrganizationsNode)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Reads organizations data and returns it as an array
			// Also supports the If-Modified-Since / Not Modified use case
			DbUser(session.userId).memberships.myOrganizations(
				context.languageIdListFor(session.userId), context.request.headers.ifModifiedSince) match {
				case Some(organizations) =>
					// May use simple model style
					Result.Success(session.modelStyle match {
						case Full => organizations.map { _.toModel }
						case Simple =>
							val descriptionRoles = DbDescriptionRoles.all
							organizations.map { _.toSimpleModelUsing(descriptionRoles) }
					})
				case None => Result.NotModified
			}
		}
	}
}

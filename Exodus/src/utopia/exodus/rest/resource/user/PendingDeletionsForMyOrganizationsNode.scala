package utopia.exodus.rest.resource.user

import utopia.access.http.Method.Get
import utopia.exodus.database.access.many.DbOrganizations
import utopia.exodus.database.access.single.DbUser
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing pending deletions in the current user's organizations
  * @author Mikko Hilpinen
  * @since 16.5.2020, v2
  */
object PendingDeletionsForMyOrganizationsNode extends Resource[AuthorizedContext]
{
	override val name = "pending"
	
	override val allowedMethods = Vector(Get)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.sessionKeyAuthorized { (session, connection) =>
			implicit val c: Connection = connection
			// Reads all user organization ids and pending deletions targeted towards those ids
			val organizationIds = DbUser(session.userId).memberships.organizationIds
			val pendingDeletions = DbOrganizations.withIds(organizationIds.toSet).deletions.pending.all
			Result.Success(pendingDeletions.map { _.toModel })
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) = Error(message = Some(
		"pending deletions doesn't have any sub-nodes at this time"))
}

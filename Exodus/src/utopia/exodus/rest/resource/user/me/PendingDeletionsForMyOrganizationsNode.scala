package utopia.exodus.rest.resource.user.me

import utopia.access.http.Method.Get
import utopia.citadel.database.access.many.organization.DbOrganizations
import utopia.exodus.model.enumeration.ExodusScope.ReadOrganizationData
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
 * Used for accessing pending deletions in the current user's organizations
 * @author Mikko Hilpinen
 * @since 16.5.2020, v1
 */
object PendingDeletionsForMyOrganizationsNode extends LeafResource[AuthorizedContext]
{
	// ATTRIBUTES   ------------------------
	
	override val name = "pending"
	override val allowedMethods = Vector(Get)
	
	
	// IMPLEMENTED  ------------------------
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.authorizedForScope(ReadOrganizationData) { (token, connection) =>
			implicit val c: Connection = connection
			// Reads all user organization ids and pending deletions targeted towards those ids
			val organizationIds = token.userAccess match {
				case Some(access) => access.memberships.organizationIds
				case None => Vector()
			}
			val pendingDeletions = {
				if (organizationIds.nonEmpty)
					DbOrganizations(organizationIds.toSet).deletions.notCancelled.pull
				else
					Vector()
			}
			Result.Success(pendingDeletions.map { _.toModel })
		}
	}
}

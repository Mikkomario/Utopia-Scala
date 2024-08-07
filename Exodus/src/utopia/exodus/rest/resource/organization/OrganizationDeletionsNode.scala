package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Delete
import utopia.citadel.database.access.single.organization.DbOrganization
import utopia.exodus.model.enumeration.ExodusTask.CancelOrganizationDeletion
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing & altering organization deletions
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
case class OrganizationDeletionsNode(organizationId: Int) extends LeafResource[AuthorizedContext]
{
	override val name = "deletions"
	override val allowedMethods = Single(Delete)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.authorizedForTask(organizationId, CancelOrganizationDeletion.id) { (session, _, connection) =>
			implicit val c: Connection = connection
			// Cancels all deletions targeted towards this organization
			val createdCancellations = DbOrganization(organizationId).deletions.notCancelled.cancel(session.ownerId)
			Result.Success(createdCancellations.map { _.toModel }.toVector)
		}
	}
}

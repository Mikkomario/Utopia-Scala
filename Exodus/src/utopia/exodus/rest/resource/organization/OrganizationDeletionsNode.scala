package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Delete
import utopia.exodus.database.access.single.DbOrganization
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.TaskType.CancelOrganizationDeletion
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.Error
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for accessing & altering organization deletions
  * @author Mikko Hilpinen
  * @since 16.5.2020, v2
  */
case class OrganizationDeletionsNode(organizationId: Int) extends Resource[AuthorizedContext]
{
	override val name = "deletions"
	
	override val allowedMethods = Vector(Delete)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		context.authorizedForTask(organizationId, CancelOrganizationDeletion) { (session, _, connection) =>
			implicit val c: Connection = connection
			// Cancels all deletions targeted towards this organization
			val updatedDeletions = DbOrganization(organizationId).deletions.pending.cancel(session.userId)
			Result.Success(updatedDeletions.map { _.toModel })
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) = Error(
		message = Some("Deletions currently has no sub-nodes"))
}

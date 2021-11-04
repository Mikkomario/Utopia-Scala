package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Delete
import utopia.access.http.Status.Forbidden
import utopia.citadel.database.access.single.organization.{DbMembership, DbOrganization}
import utopia.citadel.database.model.organization.OrganizationDeletionModel
import utopia.citadel.model.enumeration.StandardUserRole.Owner
import utopia.exodus.model.enumeration.StandardTask.DeleteOrganization
import utopia.exodus.rest.resource.scalable.{ExtendableOrganizationResource, ExtendableOrganizationResourceFactory, OrganizationUseCaseImplementation}
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.metropolis.model.partial.organization.OrganizationDeletionData
import utopia.nexus.rest.scalable.FollowImplementation
import utopia.nexus.result.Result
import utopia.vault.database.Connection

object OrganizationNode extends ExtendableOrganizationResourceFactory[OrganizationNode]
{
	override protected def buildBase(param: Int) = new OrganizationNode(param)
}

/**
  * A rest resource for accessing individual organization's data
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  */
class OrganizationNode(organizationId: Int) extends ExtendableOrganizationResource(organizationId)
{
	// ATTRIBUTES   ------------------------
	
	override protected lazy val defaultFollowImplementations = Vector(
		OrganizationInvitationsNode(organizationId),
		OrganizationDescriptionsNode(organizationId),
		OrganizationMembersNode(organizationId),
		OrganizationDeletionsNode(organizationId)
	).map { FollowImplementation.withChild(_) }
	
	private val defaultDelete = OrganizationUseCaseImplementation
		.default(Delete) { (session, membershipId, connection, _, _) =>
			implicit val c: Connection = connection
			if (DbMembership(membershipId).allowsTaskWithId(DeleteOrganization.id))
			{
				// Checks whether there already exists a pending deletion
				val organizationAccess = DbOrganization(organizationId)
				val existingDeletions = organizationAccess.deletions.notCancelled.pull
				val deletion =
				{
					if (existingDeletions.nonEmpty)
						existingDeletions.head.deletion
					else
					{
						// Calculates the deletion period (how long this action can be cancelled) based on the number of
						// organization owners and users
						val numberOfOwners = organizationAccess.membershipsWithRoles
							.limitedToRoleWithId(Owner.id).ids.size
						val organizationSize = organizationAccess.memberships.size
						// Owners (other than requester) delay deletion by a week, normal users by a day
						// Maximum wait duration is 30 days, however
						val waitDays = ((numberOfOwners - 1) * 7 + (organizationSize - numberOfOwners + 1)) min 30
						// Inserts a new deletion
						OrganizationDeletionModel
							.insert(OrganizationDeletionData(organizationId, Now + waitDays.days, session.userId))
					}
				}
				Result.Success(deletion.toModel)
			}
			else
				Result.Failure(Forbidden, "You're not allowed to perform this action")
		}
	
	override protected val defaultUseCaseImplementations = Vector(defaultDelete)
	
	
	// IMPLEMENTED  ------------------------
	
	override def name = organizationId.toString
}
package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Delete
import utopia.access.http.Status.Forbidden
import utopia.exodus.database.access.single.{DbMembership, DbOrganization}
import utopia.exodus.model.enumeration.StandardTask.DeleteOrganization
import utopia.exodus.model.enumeration.StandardUserRole.Owner
import utopia.exodus.rest.resource.scalable.{ExtendableOrganizationResource, ExtendableOrganizationResourceFactory, OrganizationUseCaseImplementation}
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.TimeExtensions._
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
				val organization = DbOrganization(organizationId)
				val existingDeletions = organization.deletions.pending.all
				val deletion =
				{
					if (existingDeletions.nonEmpty)
						existingDeletions.head.deletion
					else
					{
						// Calculates the deletion period (how long this action can be cancelled) based on the number of
						// organization owners and users
						val numberOfOwners = organization.memberships.withRole(Owner.id).size
						val organizationSize = organization.memberships.size
						// Owners (other than requester) delay deletion by a week, normal users by a day
						// Maximum wait duration is 30 days, however
						val waitDays = ((numberOfOwners - 1) * 7 + (organizationSize - numberOfOwners + 1)) min 30
						// Inserts a new deletion
						organization.deletions.insert(session.userId, waitDays.days)
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
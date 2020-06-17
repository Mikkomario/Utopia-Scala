package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Delete
import utopia.access.http.Status.Forbidden
import utopia.exodus.database.access.many.DbUserRoles
import utopia.exodus.database.access.single.{DbMembership, DbOrganization, DbUser}
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.enumeration.TaskType.RemoveMember
import utopia.metropolis.model.enumeration.UserRole.Owner
import utopia.nexus.http.Path
import utopia.nexus.rest.Resource
import utopia.nexus.rest.ResourceSearchResult.{Error, Follow}
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * A rest-resource for targeting organization members
  * @author Mikko Hilpinen
  * @since 11.5.2020, v2
  * @param organizationId Id of the targeted organization
  * @param userId Id of targeted user. None if self.
  */
case class MemberNode(organizationId: Int, userId: Option[Int]) extends Resource[AuthorizedContext]
{
	override def name = userId match
	{
		case Some(id) => id.toString
		case None => "me"
	}
	
	override val allowedMethods = Vector(Delete)
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// The user needs to be authorized for the task
		context.authorizedForTask(organizationId, RemoveMember) { (session, membershipId, connection) =>
			implicit val c: Connection = connection
			// Checks whether request targets self or other user
			userId.filterNot { _ == session.userId } match
			{
				case Some(targetUserId) =>
					// Finds targeted membership id
					DbUser(targetUserId).membershipIdInOrganizationWithId(organizationId).pull match
					{
						case Some(targetMembershipId) =>
							// Checks the roles of the active and targeted user. Targeted user can only be removed if they
							// have a role lower than the active user's
							val activeUserRoles = DbMembership(membershipId).roles.toSet
							val targetUserRoles = DbMembership(targetMembershipId).roles.toSet
							if (activeUserRoles.forall(targetUserRoles.contains))
								Result.Failure(Forbidden, s"User $targetUserId has same or higher role as you do")
							else
							{
								val managedRoles = DbUserRoles.belowOrEqualTo(activeUserRoles)
								targetUserRoles.find { !managedRoles.contains(_) } match
								{
									case Some(conflictingRole) => Result.Failure(Forbidden,
										s"You don't have the right to remove members of role ${conflictingRole.id}")
									case None =>
										// If rights are OK, ends the targeted membership
										DbMembership(targetMembershipId).end()
										Result.Empty
								}
							}
						case None => Result.Empty
					}
				case None =>
					// A user may freely remove themselves from an organization, except that the owner must leave
					// another owner behind
					if (DbMembership(membershipId).hasRole(Owner))
					{
						if (DbOrganization(organizationId).memberships.withRole(Owner).forall { _.id == membershipId })
							Result.Failure(Forbidden, "You must assign another user as organization owner before you leave.")
						else
						{
							DbMembership(membershipId).end()
							Result.Empty
						}
					}
					else
					{
						DbMembership(membershipId).end()
						Result.Empty
					}
			}
		}
	}
	
	override def follow(path: Path)(implicit context: AuthorizedContext) =
	{
		if (path.head ~== "roles")
			Follow(MemberRolesNode(organizationId, userId), path.tail)
		else
			Error(message = Some(
				"Organization member only has sub-resource 'roles'"))
	}
}

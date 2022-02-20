package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.Delete
import utopia.access.http.Status.{Forbidden, Unauthorized}
import utopia.citadel.database.access.id.many.DbUserRoleIds
import utopia.citadel.database.access.single.organization.{DbMembership, DbOrganization}
import utopia.citadel.database.access.single.user.DbUser
import utopia.citadel.model.enumeration.StandardUserRole.Owner
import utopia.exodus.model.enumeration.ExodusScope.PersonalActions
import utopia.exodus.model.enumeration.StandardTask.RemoveMember
import utopia.exodus.rest.util.AuthorizedContext
import utopia.nexus.http.Path
import utopia.nexus.rest.ResourceWithChildren
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * A rest-resource for targeting organization members
  * @author Mikko Hilpinen
  * @since 11.5.2020, v1
  * @param organizationId Id of the targeted organization
  * @param userId Id of targeted user. None if self.
  */
case class MemberNode(organizationId: Int, userId: Option[Int]) extends ResourceWithChildren[AuthorizedContext]
{
	override val allowedMethods = Vector(Delete)
	
	override def name = userId match
	{
		case Some(id) => id.toString
		case None => "me"
	}
	
	override def children = Vector(MemberRolesNode(organizationId, userId))
	
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// The user needs to be authorized for the task
		context.authorizedForTask(organizationId, RemoveMember.id) { (token, membershipId, connection) =>
			implicit val c: Connection = connection
			lazy val ownMembershipAccess = DbMembership(membershipId)
			// Checks whether request targets self or other user
			userId.filterNot { token.ownerId.contains(_) } match {
				case Some(targetUserId) =>
					// Finds targeted membership id
					DbUser(targetUserId).membershipInOrganizationWithId(organizationId).id match {
						// Case: Member of this organization
						case Some(targetMembershipId) =>
							val targetMembershipAccess = DbMembership(targetMembershipId)
							// Checks the roles of the active and targeted user.
							// Targeted user can only be removed if they
							// have a role lower or equal than the active user's
							// Special case: Owner's can't be removed except by themselves
							val targetUserRoleIds = targetMembershipAccess.roleIds.toSet
							if (targetUserRoleIds.contains(Owner.id))
								Result.Failure(Forbidden, "Owners can't be removed, they can only leave")
							else {
								val activeUserRoleIds = ownMembershipAccess.roleIds.toSet
								val managedRoleIds = DbUserRoleIds.belowOrEqualTo(activeUserRoleIds)
								
								if (targetUserRoleIds.exists { !managedRoleIds.contains(_) })
									Result.Failure(Forbidden, s"User $targetUserId has higher role than you do")
								else
								{
									// If rights are OK, ends the targeted membership
									targetMembershipAccess.end()
									Result.Empty
								}
							}
						// Case: Not a member of this organization => acts as if the user was removed
						case None => Result.Empty
					}
				case None =>
					// Makes sure the request is authorized to target the current user, also
					if (token.access.hasScope(PersonalActions)) {
						// A user may freely remove themselves from an organization,
						// except that the owner must leave another owner behind
						if (DbMembership(membershipId).hasRoleWithId(Owner.id) &&
							DbOrganization(organizationId).ownerMemberships.ids.forall { _ == membershipId })
						{
							Result.Failure(Forbidden,
								"You must assign another user as organization owner before you leave.")
						}
						else {
							ownMembershipAccess.end()
							Result.Empty
						}
					}
					else
						Result.Failure(Unauthorized,
							"Your current session doesn't authorize you to leave organizations")
			}
		}
	}
}

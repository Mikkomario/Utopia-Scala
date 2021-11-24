package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.{Delete, Post, Put}
import utopia.access.http.Status.{BadRequest, Forbidden, NotFound}
import utopia.citadel.database.access.many.organization.DbUserRoleRights
import utopia.citadel.database.access.single.organization.{DbMembership, DbOrganization}
import utopia.citadel.database.access.single.user.DbUser
import utopia.citadel.model.enumeration.StandardUserRole.Owner
import utopia.exodus.model.enumeration.StandardTask.ChangeRoles
import utopia.exodus.rest.util.AuthorizedContext
import utopia.flow.generic.ValueConversions._
import utopia.nexus.http.Path
import utopia.nexus.rest.LeafResource
import utopia.nexus.result.Result
import utopia.vault.database.Connection

/**
  * Used for interacting with organization member roles
  * @author Mikko Hilpinen
  * @since 11.5.2020, v1
  * @param organizationId Id of targeted organization
  * @param userId Id of targeted user (None if self)
  */
case class MemberRolesNode(organizationId: Int, userId: Option[Int]) extends LeafResource[AuthorizedContext]
{
	override val name = "roles"
	override val allowedMethods = Vector(Post, Put, Delete)
	
	private def organizationAccess = DbOrganization(organizationId)
	
	// TODO: Editing rights should be checked based on allowed task ids, not role ids
	override def toResponse(remainingPath: Option[Path])(implicit context: AuthorizedContext) =
	{
		// All methods require proper task-authorization
		context.authorizedForTask(organizationId, ChangeRoles.id) { (session, membershipId, connection) =>
			implicit val c: Connection = connection
			// Parses an array of role ids from the request body
			context.handleArrayPost { values =>
				val roleIds = values.flatMap { _.int }.toSet
				if (roleIds.isEmpty)
					Result.Failure(BadRequest, "Please specify one or more valid user role ids in the request json body")
				else
				{
					val method = context.request.method
					val membershipAccess = DbMembership(membershipId)
					val activeUserRoleIds = membershipAccess.roleIds.toSet
					val activeUserTaskIds = DbUserRoleRights.withAnyOfRoles(activeUserRoleIds).taskIds.toSet
					val forbiddenRoleIds = DbUserRoleRights.outsideTasks(activeUserTaskIds).roleIds.toSet
					
					// Checks whether self or another user was targeted
					userId.filterNot { _ == session.userId } match
					{
						case Some(targetUserId) =>
							DbUser(targetUserId).membershipInOrganizationWithId(organizationId).id match
							{
								case Some(targetMembershipId) =>
									// Can only modify the roles of a user that has same or lower role
									// (Except for the owner role, who can't edit another owner's roles)
									// Also, can only add or delete those roles that the active user has themselves
									val illegalRoleModifications = roleIds & forbiddenRoleIds
									if (illegalRoleModifications.nonEmpty)
										Result.Failure(Forbidden, s"You cannot modify following user roles: [${
											illegalRoleModifications.toVector.sorted.mkString(", ")}]")
									else
									{
										val targetMembershipAccess = DbMembership(targetMembershipId)
										val targetUserRoleIds = targetMembershipAccess.roleIds.toSet
										val targetUserTaskIds = DbUserRoleRights.withAnyOfRoles(targetUserRoleIds)
											.taskIds.toSet
										if (targetUserTaskIds.exists { !activeUserTaskIds.contains(_) })
											Result.Failure(Forbidden,
												s"User $targetUserId has higher role than you do")
										else if (targetUserRoleIds.contains(Owner.id))
											Result.Failure(Forbidden, "You can't edit another owner's roles")
										else
										{
											// Performs the actual changes to the roles, according to method
											// and listed roles
											// Case: POST => Adds new roles
											if (method == Post)
											{
												val newRoleIds = roleIds -- targetUserRoleIds
												if (newRoleIds.isEmpty)
													Result.Success(targetUserRoleIds.toVector.sorted)
												else
												{
													// Adds new roles to the targeted user
													targetMembershipAccess
														.assignRolesWithIds(newRoleIds, session.userId)
													Result.Success((targetUserRoleIds ++ newRoleIds).toVector.sorted)
												}
											}
											// Case: DELETE => Removes roles
											else if (method == Delete)
											{
												// The target user must be left with at least 1 role
												val rolesToRemove = targetUserRoleIds & roleIds
												if (rolesToRemove.size == targetUserRoleIds.size)
													Result.Failure(Forbidden,
														"The targeted user must be left with at least 1 role")
												else
												{
													// Removes the roles
													DbMembership(targetMembershipId)
														.removeRolesWithIds(rolesToRemove)
													Result.Success((targetUserRoleIds -- rolesToRemove)
														.toVector.sorted)
												}
											}
											// CASE: PUT => Replaces roles
											else
											{
												if (targetUserRoleIds == roleIds)
													Result.Success(targetUserRoleIds.toVector.sorted)
												else
												{
													// Adds & Removes roles to match the posted list
													val rolesToAssign = roleIds -- targetUserRoleIds
													val rolesToRemove = targetUserRoleIds -- roleIds
													if (rolesToRemove.nonEmpty)
														DbMembership(targetMembershipId)
															.removeRolesWithIds(rolesToRemove)
													if (rolesToAssign.nonEmpty)
														DbMembership(targetMembershipId)
															.assignRolesWithIds(rolesToAssign, session.userId)
													Result.Success(roleIds.toVector.sorted)
												}
											}
										}
									}
								case None => Result.Failure(NotFound,
									s"The organization doesn't have a member with user id $targetUserId")
							}
						// Case: Targeting self => Only allows DELETE and PUT, with some limitations
						// (Not allowed to delete all own roles,
						// not allowed to delete ownership without leaving another owner, not allowed to promote self)
						case None =>
							if (method == Delete) {
								val roleIdsToRemove = activeUserRoleIds & roleIds
								// Case: User didn't have any of the specified roles => Returns OK
								// Because from the client's perspective, those roles are as good as removed
								if (roleIdsToRemove.isEmpty)
									Result.Success(activeUserRoleIds.toVector.sorted)
								// Case: Yielding ownership without leaving another owner behind => fails
								else if (roleIdsToRemove.contains(Owner.id) &&
									organizationAccess.ownerMemberships.ids.forall { _ == membershipId })
									Result.Failure(Forbidden,
										"You must specify another organization owner before leaving the owner role")
								// Case: Attempting to remove every role => fails
								else if (roleIdsToRemove.size == activeUserRoleIds.size)
									Result.Failure(Forbidden, "You must leave at least one role")
								// Case: Valid request => fulfills it
								else {
									// Removes the roles and returns remaining role ids
									DbMembership(membershipId).removeRolesWithIds(roleIdsToRemove)
									Result.Success((activeUserRoleIds -- roleIdsToRemove).toVector.sorted)
								}
							}
							else if (method == Put) {
								// Makes sure not trying to add any forbidden role
								val forbiddenAssignments = forbiddenRoleIds & roleIds
								if (forbiddenAssignments.nonEmpty)
									Result.Failure(Forbidden, s"You can't add following roles: [${
										forbiddenAssignments.toVector.sorted}]")
								// Makes sure not removing all roles
								else if (roleIds.isEmpty)
									Result.Failure(Forbidden, "You must leave at least one role")
								// Checks if trying to yield ownership without leaving another owner behind
								else if (activeUserRoleIds.contains(Owner.id) && !roleIds.contains(Owner.id) &&
									organizationAccess.ownerMemberships.ids.forall { _ == membershipId })
									Result.Failure(Forbidden,
										"You must assign another owner before leaving the owner role")
								// Checks if already has targeted roles
								else if (activeUserRoleIds == roleIds)
									Result.Success(activeUserRoleIds.toVector.sorted)
								else {
									// Adds & Removes roles to match the posted list
									val rolesToAssign = roleIds -- activeUserRoleIds
									val rolesToRemove = activeUserRoleIds -- roleIds
									if (rolesToRemove.nonEmpty)
										membershipAccess.removeRolesWithIds(rolesToRemove)
									if (rolesToAssign.nonEmpty)
										membershipAccess.assignRolesWithIds(rolesToAssign, session.userId)
									Result.Success(roleIds.toVector.sorted)
								}
							}
							else
								Result.Failure(Forbidden,
									"You cannot edit your own roles (you can only remove some of them)")
					}
				}
			}
		}
	}
}

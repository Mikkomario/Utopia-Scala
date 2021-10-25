package utopia.exodus.rest.resource.organization

import utopia.access.http.Method.{Delete, Post, Put}
import utopia.access.http.Status.{BadRequest, Forbidden, NotFound}
import utopia.citadel.database.access.id.many.DbUserRoleIds
import utopia.citadel.database.access.single.organization.DbMembership
import utopia.citadel.database.access.single.user.DbUser
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
					// Checks whether self or another user was targeted
					userId.filterNot { _ == session.userId } match
					{
						case Some(targetUserId) =>
							DbUser(targetUserId).membershipInOrganizationWithId(organizationId).id match
							{
								case Some(targetMembershipId) =>
									// Can only modify the roles of a user that has a lower role
									// Also, can only add or delete those roles that the active user has themselves
									val activeUserRoleIds = DbMembership(membershipId).roleIds.toSet
									val illegalRoleModifications = roleIds -- activeUserRoleIds
									if (illegalRoleModifications.nonEmpty)
										Result.Failure(Forbidden, s"You cannot modify following user roles: [${
											illegalRoleModifications.toVector.sorted.mkString(", ")}]")
									else
									{
										val targetMembershipAccess = DbMembership(targetMembershipId)
										val targetUserRoleIds = targetMembershipAccess.roleIds.toSet
										if (activeUserRoleIds.forall(targetUserRoleIds.contains))
											Result.Failure(Forbidden,
												s"User $targetUserId has same or higher role as you do")
										else
										{
											val managedRoleIds = DbUserRoleIds.belowOrEqualTo(activeUserRoleIds)
											targetUserRoleIds.find { !managedRoleIds.contains(_) } match
											{
												case Some(conflictingRole) => Result.Failure(Forbidden,
													s"You don't have the right to adjust the roles of users with role $conflictingRole")
												case None =>
													// Performs the actual changes to the roles, according to method
													// and listed roles
													val method = context.request.method
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
															Result.Success((targetUserRoleIds ++ newRoleIds)
																.toVector.sorted)
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
									}
								case None => Result.Failure(NotFound,
									s"The organization doesn't have a member with user id $targetUserId")
							}
						case None => Result.Failure(Forbidden, "You cannot edit your own roles")
					}
				}
			}
		}
	}
}

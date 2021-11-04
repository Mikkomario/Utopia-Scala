package utopia.citadel.database.access.single.organization

import utopia.citadel.database.access.many.organization.{DbMemberRoleLinks, DbMemberRolesWithRights, DbUserRoleRights, ManyMemberRolesWithRightsAccess}
import utopia.citadel.database.factory.organization.MembershipWithRolesFactory
import utopia.citadel.database.model.organization.MemberRoleLinkModel
import utopia.metropolis.model.combined.organization.MembershipWithRoles
import utopia.metropolis.model.partial.organization.MemberRoleLinkData
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual Memberships, based on their id
  * @since 2021-10-23
  */
case class DbSingleMembership(id: Int) extends UniqueMembershipAccess with SingleIntIdModelAccess[Membership]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return An access point to this membership's role links
	  */
	def roleLinks = DbMemberRoleLinks.withMembershipId(id)
	/**
	  * @return An access point to this memberhip's role links, which include allowed task ids
	  */
	def roleLinksWithRights: ManyMemberRolesWithRightsAccess = DbMemberRolesWithRights.withMembershipId(id)
	
	/**
	  * @return An access point to this membership with the roles included
	  */
	def withRoles = DbSingleMembershipWithRoles(id)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return ids of the user roles this member has access to
	  */
	def roleIds(implicit connection: Connection) = roleLinks.roleIds
	/**
	  * @param connection Implicit DB Connection
	  * @return Ids of the tasks allowed by this membership
	  */
	def allowedTaskIds(implicit connection: Connection) = roleLinksWithRights.taskIds
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param userRoleId Id of the searched user role
	  * @param connection Implicit DB Connection
	  * @return Whether this membership includes that role
	  */
	def hasRoleWithId(userRoleId: Int)(implicit connection: Connection) = roleLinks.containsRoleWithId(userRoleId)
	/**
	  * @param taskId Id of the tested task
	  * @param connection Implicit DB connection
	  * @return Whether this membership allows execution of the specified task
	  */
	def allowsTaskWithId(taskId: Int)(implicit connection: Connection) =
		roleLinksWithRights.allowsTaskWithId(taskId)
	
	/**
	  * @param userRoleId A user role
	  * @param connection Implicit DB Connection
	  * @return Whether this member is allowed to promote another user to that role
	  */
	def canPromoteToRoleWithId(userRoleId: Int)(implicit connection: Connection) =
	{
		val availableTaskIds = allowedTaskIds
		val requiredTaskIds = DbUserRoleRights.withRoleId(userRoleId).taskIds.toSet
		requiredTaskIds.forall(availableTaskIds.contains)
	}
	
	/**
	  * Adds a new user role to this membership. Please make sure the new role is not a duplicate of an existing role.
	  * @param newRoleId Id of the new user role (must not be duplicate)
	  * @param creatorId Id of the user who assigns this role
	  * @param connection Implicit DB Connection
	  * @return Newly assigned role link
	  */
	def assignRoleWithId(newRoleId: Int, creatorId: Int)(implicit connection: Connection) =
		MemberRoleLinkModel.insert(MemberRoleLinkData(id, newRoleId, Some(creatorId)))
	/**
	  * Adds multiple new user roles to this membership. Please make sure only new roles are added
	  * (no duplicates with existing roles)
	  * @param newRoleIds Ids of the roles to assign (mustn't be duplicates with existing roles)
	  * @param creatorId Id of the user who assigns these roles
	  * @param connection Implicit DB Connection
	  * @return Newly assigned role links
	  */
	def assignRolesWithIds(newRoleIds: Set[Int], creatorId: Int)(implicit connection: Connection) =
		MemberRoleLinkModel.insert(newRoleIds.toVector.sorted.map { roleId => MemberRoleLinkData(id, roleId, Some(creatorId)) })
	/**
	  * Removes the specified roles from this membership
	  * @param userRoleIds Targeted user role ids
	  * @param connection Implicit DB connection
	  * @return Whether this membership was affected
	  */
	def removeRolesWithIds(userRoleIds: Iterable[Int])(implicit connection: Connection) =
		roleLinks.withAnyOfRoles(userRoleIds).deprecate()
}

case class DbSingleMembershipWithRoles(id: Int) extends SingleIntIdModelAccess[MembershipWithRoles]
{
	// IMPLEMENTED  ------------------------------
	
	override def factory = MembershipWithRolesFactory
}

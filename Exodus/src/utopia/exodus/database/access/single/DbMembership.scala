package utopia.exodus.database.access.single

import utopia.exodus.database.Tables
import utopia.exodus.database.access.many.DbTaskTypes
import utopia.exodus.database.factory.organization.{MembershipFactory, RoleRightFactory}
import utopia.exodus.database.model.organization.{MemberRoleModel, MembershipModel, RoleRightModel}
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.{TaskType, UserRole}
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleIdModelAccess, SingleModelAccess}
import utopia.vault.sql.{Exists, Select, SelectDistinct, Where}
import utopia.vault.sql.Extensions._

/**
  * An access point to individual memberships and their data
  * @author Mikko Hilpinen
  * @since 5.5.2020, v2
  */
object DbMembership extends SingleModelAccess[Membership]
{
	// IMPLEMENTED	--------------------------
	
	override def factory = MembershipFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	------------------------------
	
	private def model = MembershipModel
	
	
	// OTHER	------------------------------
	
	/**
	  * @param id A membership id
	  * @return An access point to that membership
	  */
	def apply(id: Int) = new SingleMembership(id)
	
	
	// NESTED	------------------------------
	
	class SingleMembership(membershipId: Int) extends SingleIdModelAccess(membershipId, factory)
	{
		// IMPLEMENTED	----------------------
		
		override val factory = DbMembership.factory
		
		
		// COMPUTED	--------------------------
		
		private def memberRoleFactory = MemberRoleModel
		
		private def rightsFactory = RoleRightFactory
		
		private def rightsModel = RoleRightModel
		
		private def rightsTarget = memberRoleFactory.table join Tables.userRole join rightsFactory.table
		
		/**
		  * @param connection DB Connection (implicit)
		  * @return Roles assigned to this user in this membership
		  */
		def roles(implicit connection: Connection) =
		{
			connection(Select.index(memberRoleFactory.table) + Where(rolesCondition)).rowIntValues.flatMap { roleId =>
				UserRole.forId(roleId).toOption }
		}
		
		/**
		  * @param connection DB Connection (implicit)
		  * @return All tasks that are allowed through this membership
		  */
		def allowedActions(implicit connection: Connection) =
		{
			// Joins in the tasks link table and selects unique task ids
			connection(SelectDistinct(rightsTarget, rightsModel.taskIdColumn) + Where(rolesCondition)).rowIntValues
				.flatMap { taskId => TaskType.forId(taskId).toOption }
		}
		
		private def rolesCondition = memberRoleFactory.withMembershipId(membershipId).toCondition &&
			memberRoleFactory.nonDeprecatedCondition
		
		
		// OTHER	---------------------------
		
		/**
		  * Checks whether this user has the specified role as a part of this membership
		  * @param role Checked role
		  * @param connection DB Connection (implicit)
		  * @return Whether this membership is associated with specified role
		  */
		def hasRole(role: UserRole)(implicit connection: Connection) =
			Exists(memberRoleFactory.table, rolesCondition && memberRoleFactory.withRole(role).toCondition)
		
		/**
		  * Assigns new roles to this membership. <b>Please make sure all assigned roles are actually new,
		  * since no such check is made here</b>
		  * @param newRoles New roles to assign to this membership
		  * @param creatorId Id of the user who added these roles
		  * @param connection DB Connection (implicit)
		  */
		def assignRoles(newRoles: Set[UserRole], creatorId: Int)(implicit connection: Connection) =
			newRoles.foreach { role => memberRoleFactory.insert(membershipId, role, creatorId) }
		
		/**
		  * @param rolesToRemove Roles that should be removed from this membership
		  * @param connection DB Connection (implicit)
		  * @return Number of roles that were removed
		  */
		def removeRoles(rolesToRemove: Set[UserRole])(implicit connection: Connection) =
		{
			memberRoleFactory.nowDeprecated.updateWhere(rolesCondition &&
				memberRoleFactory.roleIdColumn.in(rolesToRemove.map { _.id }))
		}
		
		/**
		  * Checks whether this membership allows the specified action
		  * @param action Action the user would like to perform
		  * @param connection DB Connection (implicit)
		  * @return Whether the user/member is allowed to perform the specified task in this organization/membership
		  */
		def allows(action: TaskType)(implicit connection: Connection) =
		{
			Exists(rightsTarget, rolesCondition && rightsModel.withTask(action).toCondition)
		}
		
		/**
		  * Checks whether this organization member can promote another user to specified role. This member needs to
		  * have all the rights the targeted role would have
		  * @param targetRole A role a user is being promoted to
		  * @param connection DB Connection (implicit)
		  * @return Whether this member has the authorization to promote a user to that role
		  */
		def canPromoteTo(targetRole: UserRole)(implicit connection: Connection) =
		{
			// Uses multiple requests since the join logic is rather complex
			val myTasks = allowedActions.toSet
			val requiredTasks = DbTaskTypes.forRole(targetRole).toSet
			requiredTasks.forall(myTasks.contains)
		}
		
		/**
		  * @param connection DB Connection (implicit)
		  * @return Whether a membership was updated/changed
		  */
		def end()(implicit connection: Connection) =
		{
			// Marks this membership as ended (if not ended already)
			model.nowEnded.updateWhere(mergeCondition(factory.nonDeprecatedCondition)) > 0
		}
	}
}

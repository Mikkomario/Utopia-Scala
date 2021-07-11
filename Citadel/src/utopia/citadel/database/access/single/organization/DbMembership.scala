package utopia.citadel.database.access.single.organization

import utopia.citadel.database.Tables
import utopia.citadel.database.access.id.many.DbTaskIds
import utopia.citadel.database.factory.organization.{MembershipFactory, RoleRightFactory}
import utopia.citadel.database.model.organization.{MemberRoleModel, MembershipModel, RoleRightModel}
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.access.single.model.distinct.SingleIdModelAccess
import utopia.vault.sql.{Exists, Select, SelectDistinct, Where}
import utopia.vault.sql.SqlExtensions._

/**
  * An access point to individual memberships and their data
  * @author Mikko Hilpinen
  * @since 5.5.2020, v1.0
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
		  * @return Ids of the roles assigned to this user in this membership
		  */
		def roleIds(implicit connection: Connection) =
			connection(Select.index(memberRoleFactory.table) + Where(rolesCondition)).rowIntValues
		
		/**
		  * @param connection DB Connection (implicit)
		  * @return Ids of all tasks that are allowed through this membership
		  */
		def allowedTaskIds(implicit connection: Connection) =
		{
			// Joins in the tasks link table and selects unique task ids
			connection(SelectDistinct(rightsTarget, rightsModel.taskIdColumn) + Where(rolesCondition)).rowIntValues
		}
		
		private def rolesCondition = memberRoleFactory.withMembershipId(membershipId).toCondition &&
			memberRoleFactory.nonDeprecatedCondition
		
		
		// OTHER	---------------------------
		
		/**
		  * Checks whether this user has the specified role as a part of this membership
		  * @param roleId     Checked role id
		  * @param connection DB Connection (implicit)
		  * @return Whether this membership is associated with specified role
		  */
		def hasRoleWithId(roleId: Int)(implicit connection: Connection) =
			Exists(memberRoleFactory.table, rolesCondition && memberRoleFactory.withRoleId(roleId).toCondition)
		
		/**
		  * Assigns new roles to this membership. <b>Please make sure all assigned roles are actually new,
		  * since no such check is made here</b>
		  * @param newRoleIds Ids of the new roles to assign to this membership
		  * @param creatorId  Id of the user who added these roles
		  * @param connection DB Connection (implicit)
		  */
		def assignRoles(newRoleIds: Set[Int], creatorId: Int)(implicit connection: Connection) =
			newRoleIds.foreach { roleId => memberRoleFactory.insert(membershipId, roleId, creatorId) }
		
		/**
		  * @param roleIdsToRemove Ids of the roles that should be removed from this membership
		  * @param connection      DB Connection (implicit)
		  * @return Number of roles that were removed
		  */
		def removeRolesWithIds(roleIdsToRemove: Set[Int])(implicit connection: Connection) =
		{
			memberRoleFactory.nowDeprecated.updateWhere(rolesCondition &&
				memberRoleFactory.roleIdColumn.in(roleIdsToRemove))
		}
		
		/**
		  * Checks whether this membership allows the specified action
		  * @param taskId     Id of the task the user would like to perform
		  * @param connection DB Connection (implicit)
		  * @return Whether the user/member is allowed to perform the specified task in this organization/membership
		  */
		def allowsTaskWithId(taskId: Int)(implicit connection: Connection) =
			Exists(rightsTarget, rolesCondition && rightsModel.withTaskId(taskId).toCondition)
		
		/**
		  * Checks whether this organization member can promote another user to specified role. This member needs to
		  * have all the rights the targeted role would have
		  * @param targetRoleId Id of the role a user is being promoted to
		  * @param connection   DB Connection (implicit)
		  * @return Whether this member has the authorization to promote a user to that role
		  */
		def canPromoteToRoleWithId(targetRoleId: Int)(implicit connection: Connection) =
		{
			// Uses multiple requests since the join logic is rather complex
			val myTasks = allowedTaskIds.toSet
			val requiredTasks = DbTaskIds.forRoleWithId(targetRoleId).toSet
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

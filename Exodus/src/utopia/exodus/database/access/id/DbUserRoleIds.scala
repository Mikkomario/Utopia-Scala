package utopia.exodus.database.access.id

import utopia.exodus.database.Tables
import utopia.exodus.database.model.organization.RoleRightModel
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyIntIdAccess
import utopia.vault.sql.SqlExtensions._
import utopia.vault.sql.{SelectDistinct, Where}

/**
  * Used for accessing multiple user roles at once
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
object DbUserRoleIds extends ManyIntIdAccess
{
	// IMPLEMENTED	------------------------
	
	override def target = table
	
	override def table = Tables.userRole
	
	override def globalCondition = None
	
	
	// OTHER	----------------------------
	
	/**
	  * @param roleId A user role id
	  * @param connection DB Connection (implicit)
	  * @return An id list of user roles that have same or fewer rights that this role and can thus be considered to be
	  *         "below" this role.
	  */
	def belowOrEqualTo(roleId: Int)(implicit connection: Connection) =
		allowingOnlyTasksWithIds(DbTaskIds.forRoleWithId(roleId).toSet)
	
	/**
	  * @param roleIds A set of user role ids
	  * @param connection DB Connection
	  * @return List of roles that allow all of, or a subset of tasks allowed for any of the specified roles
	  */
	def belowOrEqualTo(roleIds: Set[Int])(implicit connection: Connection) =
		allowingOnlyTasksWithIds(DbTaskIds.forRoleCombination(roleIds).toSet)
	
	/**
	  * @param taskIds Ids of allowed tasks
	  * @param connection DB Connection (implicit)
	  * @return List of ids of the user roles that allow all of, or a subset of the specified tasks
	  */
	def allowingOnlyTasksWithIds(taskIds: Set[Int])(implicit connection: Connection) =
	{
		// Only includes roles that have only tasks within "allowed tasks" list
		val excludedRoleIds = connection(SelectDistinct(RoleRightModel.table, RoleRightModel.roleIdAttName) +
			Where.not(RoleRightModel.taskIdColumn.in(taskIds))).rowIntValues
		
		all.toSet -- excludedRoleIds
	}
}

package utopia.exodus.database.access.many

import utopia.exodus.database.factory.organization.RoleRightFactory
import utopia.exodus.database.model.organization.RoleRightModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.organization.RoleWithRights
import utopia.metropolis.model.enumeration.{TaskType, UserRole}
import utopia.vault.database.Connection
import utopia.vault.sql.{SelectDistinct, Where}
import utopia.vault.sql.Extensions._

/**
  * Used for accessing multiple user roles at once
  * @author Mikko Hilpinen
  * @since 4.5.2020, v2
  */
object DbUserRoles
{
	// COMPUTED	----------------------------
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return All user roles, along with the allowed task types
	  */
	def withRights(implicit connection: Connection) =
	{
		// Reads all role rights
		val rights = RoleRightFactory.getAll().groupBy { _.role }
		// Combines roles with rights
		UserRole.values.map { role =>
			val roleRights = rights.getOrElse(role, Vector()).map { _.task }.toSet
			RoleWithRights(role, roleRights)
		}
	}
	
	
	// OTHER	----------------------------
	
	/**
	  * @param role A user role
	  * @param connection DB Connection (implicit)
	  * @return A list of user roles that have same or fewer rights that this role and can thus be considered to be
	  *         "below" this role.
	  */
	def belowOrEqualTo(role: UserRole)(implicit connection: Connection) =
	{
		val allowedTasks = DbTaskTypes.forRole(role)
		// Only includes roles that have only tasks within "allowed tasks" list
		val excludedRoleIds = connection(SelectDistinct(RoleRightModel.table, RoleRightModel.roleIdAttName) +
			Where.not(RoleRightModel.taskIdColumn.in(allowedTasks.map { _.id }))).rowIntValues
		UserRole.values.filterNot { role => excludedRoleIds.contains(role.id) }
	}
	
	/**
	  * @param roles A set of roles
	  * @param connection DB Connection
	  * @return List of roles that allow all of, or a subset of tasks allowed for any of the specified roles
	  */
	def belowOrEqualTo(roles: Set[UserRole])(implicit connection: Connection) =
		allowingOnly(DbTaskTypes.forRoleCombination(roles).toSet)
	
	/**
	  * @param tasks Set of allowed tasks
	  * @param connection DB Connection (implicit)
	  * @return List of user roles that allow all of, or a subset of the specified tasks
	  */
	def allowingOnly(tasks: Set[TaskType])(implicit connection: Connection) =
	{
		// Only includes roles that have only tasks within "allowed tasks" list
		val excludedRoleIds = connection(SelectDistinct(RoleRightModel.table, RoleRightModel.roleIdAttName) +
			Where.not(RoleRightModel.taskIdColumn.in(tasks.map { _.id }))).rowIntValues
		UserRole.values.filterNot { role => excludedRoleIds.contains(role.id) }
	}
}

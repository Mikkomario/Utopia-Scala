package utopia.exodus.database.access.many

import utopia.exodus.database.access.id.UserRoleIds
import utopia.exodus.database.factory.organization.RoleRightFactory
import utopia.metropolis.model.combined.organization.RoleWithRights
import utopia.vault.database.Connection

/**
  * Used for accessing multiple user roles at once
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
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
		val rights = RoleRightFactory.getAll().groupBy { _.roleId }
		// Combines roles with rights
		UserRoleIds.all.map { roleId => RoleWithRights(roleId, rights.getOrElse(roleId, Set()).map { _.taskId }.toSet) }
	}
}

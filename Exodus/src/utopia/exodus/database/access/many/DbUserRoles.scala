package utopia.exodus.database.access.many

import utopia.exodus.database.access.id.UserRoleIds
import utopia.exodus.database.factory.organization.RoleRightFactory
import utopia.exodus.database.model.organization.RoleRightModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.organization.RoleWithRights
import utopia.vault.database.Connection
import utopia.vault.sql.Extensions._

/**
  * Used for accessing multiple user roles at once
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
object DbUserRoles
{
	// COMPUTED	----------------------------
	
	private def rightsFactory = RoleRightFactory
	
	private def rightsModel = RoleRightModel
	
	/**
	  * @param connection DB Connection (implicit)
	  * @return All user roles, along with the allowed task types
	  */
	def withRights(implicit connection: Connection) =
	{
		// Reads all role rights
		val rights = rightsFactory.getAll().groupBy { _.roleId }
		// Combines roles with rights
		UserRoleIds.all.map { roleId => RoleWithRights(roleId, rights.getOrElse(roleId, Set()).map { _.taskId }.toSet) }
	}
	
	/**
	  * @return An access point to descriptions concerning (all) user roles
	  */
	def descriptions = DbDescriptions.ofAllUserRoles
	
	
	// OTHER	----------------------------
	
	/**
	  * @param roleIds Targeted role ids
	  * @return An access point to data concerning those roles
	  */
	def apply(roleIds: Set[Int]) = new DbUserRolesSubgroup(roleIds)
	
	
	// NESTED	----------------------------
	
	class DbUserRolesSubgroup(roleIds: Set[Int])
	{
		/**
		  * @param connection DB connection (implicit)
		  * @return Targeted user roles, along with allowed task ids for each
		  */
		def withRights(implicit connection: Connection) =
		{
			// Reads associated role rights
			val rights = rightsFactory.getMany(rightsModel.roleIdColumn.in(roleIds)).groupBy { _.roleId }
			// Attaches the rights to role ids
			roleIds.map { roleId => RoleWithRights(roleId, rights.getOrElse(roleId, Set()).map { _.taskId }.toSet) }
		}
		
		/**
		  * @return An access point to descriptions concerning these user roles
		  */
		def descriptions = DbDescriptions.ofUserRolesWithIds(roleIds)
	}
}
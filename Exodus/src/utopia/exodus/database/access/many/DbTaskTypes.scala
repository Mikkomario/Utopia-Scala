package utopia.exodus.database.access.many

import utopia.exodus.database.factory.organization.RoleRightFactory
import utopia.exodus.database.model.organization.RoleRightModel
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.UserRole
import utopia.vault.database.Connection
import utopia.vault.sql.Extensions._

/**
  * Used for accessing multiple task types at once
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
object DbTaskTypes
{
	/**
	  * @param role A user role
	  * @param connection DB Connection (implicit)
	  * @return All task types that are accessible for that user role
	  */
	def forRole(role: UserRole)(implicit connection: Connection) =
	{
		// Reads task types from role rights
		RoleRightFactory.getMany(RoleRightModel.withRole(role).toCondition).map { _.task }
	}
	
	/**
	  * @param roles A set of roles
	  * @param connection DB Connection
	  * @return All task types that are accessible for any of these roles
	  */
	def forRoleCombination(roles: Set[UserRole])(implicit connection: Connection) =
	{
		if (roles.isEmpty)
			Vector()
		else if (roles.size == 1)
			forRole(roles.head)
		else
			RoleRightFactory.getMany(RoleRightModel.roleIdColumn.in(roles.map { _.id })).map { _.task }
	}
}

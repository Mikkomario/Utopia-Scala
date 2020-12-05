package utopia.exodus.database.access.id

import utopia.exodus.database.Tables
import utopia.exodus.database.factory.organization.RoleRightFactory
import utopia.exodus.database.model.organization.RoleRightModel
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyIntIdAccess
import utopia.vault.sql.Extensions._

/**
  * Used for accessing multiple task ids at once
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
object DbTaskIds extends ManyIntIdAccess
{
	// IMPLEMENTED	-----------------------
	
	override def target = table
	
	override def table = Tables.task
	
	override def globalCondition = None
	
	
	// OTHER	---------------------------
	
	/**
	  * @param roleId A user role id
	  * @param connection DB Connection (implicit)
	  * @return All task types that are accessible for that user role
	  */
	def forRoleWithId(roleId: Int)(implicit connection: Connection) =
	{
		// Reads task types from role rights
		RoleRightFactory.getMany(RoleRightModel.withRoleId(roleId).toCondition).map { _.taskId }
	}
	
	/**
	  * @param roleIds A set of role ids
	  * @param connection DB Connection
	  * @return All task types that are accessible for any of these roles
	  */
	def forRoleCombination(roleIds: Set[Int])(implicit connection: Connection) =
	{
		if (roleIds.isEmpty)
			Vector()
		else if (roleIds.size == 1)
			forRoleWithId(roleIds.head)
		else
			RoleRightFactory.getMany(RoleRightModel.roleIdColumn.in(roleIds)).map { _.taskId }
	}
}

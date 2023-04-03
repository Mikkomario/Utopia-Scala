package utopia.citadel.database.access.id.many

import utopia.citadel.database.CitadelTables
import utopia.citadel.database.factory.organization.UserRoleRightFactory
import utopia.citadel.database.model.organization.UserRoleRightModel
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.column.ManyIntIdAccess

/**
  * Used for accessing multiple task ids at once
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.0
  */
object DbTaskIds extends ManyIntIdAccess
{
	// IMPLEMENTED	-----------------------
	
	override def target = table
	
	override def table = CitadelTables.task
	
	override def globalCondition = None
	
	
	// OTHER	---------------------------
	
	/**
	  * @param roleId     A user role id
	  * @param connection DB Connection (implicit)
	  * @return All task types that are accessible for that user role
	  */
	def forUserRoleWithId(roleId: Int)(implicit connection: Connection) =
	{
		// Reads task types from role rights
		UserRoleRightFactory.findMany(UserRoleRightModel.withRoleId(roleId).toCondition).map { _.taskId }
	}
	
	/**
	  * @param roleIds    A set of role ids
	  * @param connection DB Connection
	  * @return All task types that are accessible for any of these roles
	  */
	def forUserRoleCombination(roleIds: Set[Int])(implicit connection: Connection) =
	{
		if (roleIds.isEmpty)
			Set[Int]()
		else if (roleIds hasSize 1)
			forUserRoleWithId(roleIds.head).toSet
		else
			UserRoleRightFactory.findMany(UserRoleRightModel.roleIdColumn.in(roleIds)).map { _.taskId }.toSet
	}
}

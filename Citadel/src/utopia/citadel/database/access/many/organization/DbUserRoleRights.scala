package utopia.citadel.database.access.many.organization

import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple UserRoleRights at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUserRoleRights extends ManyUserRoleRightsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted UserRoleRights
	  * @return An access point to UserRoleRights with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbUserRoleRightsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbUserRoleRightsSubset(targetIds: Set[Int]) extends ManyUserRoleRightsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}


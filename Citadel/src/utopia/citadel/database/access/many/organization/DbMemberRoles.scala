package utopia.citadel.database.access.many.organization

import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.MemberRole
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple MemberRoles at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbMemberRoles extends ManyMemberRolesAccess with NonDeprecatedView[MemberRole]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted MemberRoles
	  * @return An access point to MemberRoles with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbMemberRolesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbMemberRolesSubset(targetIds: Set[Int]) extends ManyMemberRolesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}


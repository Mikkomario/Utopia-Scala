package utopia.citadel.database.access.many.organization

import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.organization.MemberRoleLink
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * The root access point when targeting multiple MemberRoles at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbMemberRoleLinks extends ManyMemberRoleLinksAccess with NonDeprecatedView[MemberRoleLink]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted MemberRoles
	  * @return An access point to MemberRoles with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbMemberRoleLinksSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbMemberRoleLinksSubset(targetIds: Set[Int]) extends ManyMemberRoleLinksAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}


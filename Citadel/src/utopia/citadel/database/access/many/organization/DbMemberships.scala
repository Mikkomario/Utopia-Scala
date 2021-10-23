package utopia.citadel.database.access.many.organization

import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple Memberships at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbMemberships extends ManyMembershipsAccess with NonDeprecatedView[Membership]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Memberships
	  * @return An access point to Memberships with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbMembershipsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbMembershipsSubset(targetIds: Set[Int]) extends ManyMembershipsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}


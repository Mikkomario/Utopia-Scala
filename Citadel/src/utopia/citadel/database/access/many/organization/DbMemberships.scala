package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.organization.DbMembershipsWithRoles.DbMembershipsWithRolesWithHistory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.nosql.view.{NonDeprecatedView, UnconditionalView}
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple Memberships at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbMemberships extends ManyMembershipsAccess with NonDeprecatedView[Membership]
{
	// COMPUTED --------------------
	
	/**
	  * @return An access point to memberships, including historical ones (those not currently active)
	  */
	def withHistory = DbMembershipsWithHistory
	/**
	  * @return An access point to memberships with their role included
	  */
	def withRoles = DbMembershipsWithRoles
	
	
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Memberships
	  * @return An access point to Memberships with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbMembershipsSubset(ids)
	
	
	// NESTED	--------------------
	
	object DbMembershipsWithHistory extends ManyMembershipsAccess with UnconditionalView
	{
		/**
		  * @return A version of this access point which includes member role assignments (both current and historical)
		  */
		def withRoles = DbMembershipsWithRolesWithHistory
	}
	
	class DbMembershipsSubset(targetIds: Set[Int]) extends ManyMembershipsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}


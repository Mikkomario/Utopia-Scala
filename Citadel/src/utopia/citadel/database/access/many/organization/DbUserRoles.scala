package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.description.ManyDescribedAccessByIds
import utopia.metropolis.model.combined.organization.DescribedUserRole
import utopia.metropolis.model.stored.organization.UserRole
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple UserRoles at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUserRoles extends ManyUserRolesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted UserRoles
	  * @return An access point to UserRoles with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbUserRolesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbUserRolesSubset(override val ids: Set[Int]) 
		extends ManyUserRolesAccess with ManyDescribedAccessByIds[UserRole, DescribedUserRole]
	{
		/**
		  * @return An access point to user role rights concerning these user roles
		  */
		def rights = DbUserRoleRights.withAnyOfRoles(ids)
	}
}


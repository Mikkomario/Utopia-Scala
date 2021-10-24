package utopia.citadel.database.access.many.organization

import utopia.metropolis.model.combined.organization.MembershipWithRoles
import utopia.vault.nosql.view.{NonDeprecatedView, UnconditionalView}

/**
  * Used for accessing multiple memberships at a time, including their role information
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
object DbMembershipsWithRoles extends ManyMembershipsWithRolesAccess with NonDeprecatedView[MembershipWithRoles]
{
	// COMPUTED --------------------------------
	
	/**
	  * @return An access point to memberships (including past memberships)
	  *         with their roles included (including past roles)
	  */
	def withHistory = DbMembershipsWithRolesWithHistory
	
	
	// NESTED   --------------------------------
	
	object DbMembershipsWithRolesWithHistory extends ManyMembershipsWithRolesAccess with UnconditionalView
}

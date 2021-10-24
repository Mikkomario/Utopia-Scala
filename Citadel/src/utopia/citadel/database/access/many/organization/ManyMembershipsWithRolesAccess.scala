package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.organization.ManyMembershipsWithRolesAccess.SubAcccess
import utopia.citadel.database.factory.organization.MembershipWithRolesFactory
import utopia.citadel.database.model.organization.MemberRoleModel
import utopia.metropolis.model.combined.organization.MembershipWithRoles
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyMembershipsWithRolesAccess
{
	private class SubAcccess(override val parent: ManyModelAccess[MembershipWithRoles],
	                                     override val filterCondition: Condition)
		extends ManyMembershipsWithRolesAccess with SubView
}

/**
  * A common trait for access points which return multiple memberships, including linked user role connections
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
trait ManyMembershipsWithRolesAccess
	extends ManyMembershipsAccessLike[MembershipWithRoles, ManyMembershipsWithRolesAccess]
{
	// COMPUTED -------------------------------
	
	/**
	  * @return Model used for interacting with member role links
	  */
	protected def roleLinkModel = MemberRoleModel
	
	
	// IMPLEMENTED  --------------------------
	
	override def factory = MembershipWithRolesFactory
	override protected def defaultOrdering = Some(factory.parentFactory.defaultOrdering)
	
	override protected def _filter(condition: Condition): ManyMembershipsWithRolesAccess =
		new SubAcccess(this, condition)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param userRoleId A user role id
	  * @return An access point to memberships + member roles that refer to the specified user role
	  */
	def limitedToRoleWithId(userRoleId: Int) =
		filter(roleLinkModel.withRoleId(userRoleId).toCondition)
}

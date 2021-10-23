package utopia.citadel.database.access.single.organization

import utopia.citadel.database.access.many.organization.DbMemberRoles
import utopia.citadel.database.factory.organization.MembershipWithRolesFactory
import utopia.metropolis.model.combined.organization.MembershipWithRoles
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual Memberships, based on their id
  * @since 2021-10-23
  */
case class DbSingleMembership(id: Int) extends UniqueMembershipAccess with SingleIntIdModelAccess[Membership]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return An access point to this membership's role links
	  */
	def roleLinks = DbMemberRoles.withMembershipId(id)
	
	/**
	  * @return An access point to this membership with the roles included
	  */
	def withRoles = DbSingleMembershipWithRoles(id)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return ids of the user roles this member has access to
	  */
	def roleIds(implicit connection: Connection) = roleLinks.roleIds
}

case class DbSingleMembershipWithRoles(id: Int) extends SingleIntIdModelAccess[MembershipWithRoles]
{
	// IMPLEMENTED  ------------------------------
	
	override def factory = MembershipWithRolesFactory
}

package utopia.citadel.database.factory.organization

import utopia.metropolis.model.combined.organization.MembershipWithRoles
import utopia.metropolis.model.stored.organization.{MemberRoleLink, Membership}
import utopia.vault.nosql.factory.multi.MultiCombiningFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading rich membership data from DB
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1.0
  */
object MembershipWithRolesFactory
	extends MultiCombiningFactory[MembershipWithRoles, Membership, MemberRoleLink] with Deprecatable
{
	// IMPLEMENTED	----------------------------
	
	override def parentFactory = MembershipFactory
	
	override def childFactory = MemberRoleLinkFactory
	
	override def nonDeprecatedCondition = parentFactory.nonDeprecatedCondition &&
		childFactory.nonDeprecatedCondition
	
	override def isAlwaysLinked = false
	
	override def apply(parent: Membership, children: Vector[MemberRoleLink]) = MembershipWithRoles(parent, children.toSet)
}

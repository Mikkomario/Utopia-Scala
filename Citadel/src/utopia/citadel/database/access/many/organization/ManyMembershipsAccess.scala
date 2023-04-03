package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.MembershipFactory
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyMembershipsAccess
{
	// NESTED	--------------------
	
	private class ManyMembershipsSubView(override val parent: ManyRowModelAccess[Membership], 
		override val filterCondition: Condition) 
		extends ManyMembershipsAccess with SubView
}

/**
  * A common trait for access points which target multiple Memberships at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyMembershipsAccess
	extends ManyMembershipsAccessLike[Membership, ManyMembershipsAccess] with ManyRowModelAccess[Membership]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = MembershipFactory
	
	override def _filter(additionalCondition: Condition): ManyMembershipsAccess =
		new ManyMembershipsAccess.ManyMembershipsSubView(this, additionalCondition)
}


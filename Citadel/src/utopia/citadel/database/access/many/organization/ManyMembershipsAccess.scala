package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.MembershipFactory
import utopia.metropolis.model.stored.organization.Membership
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.Condition

object ManyMembershipsAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyMembershipsAccess = SubAccess(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class SubAccess(accessCondition: Option[Condition]) extends ManyMembershipsAccess
}

/**
  * A common trait for access points which target multiple Memberships at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyMembershipsAccess 
	extends ManyMembershipsAccessLike[Membership, ManyMembershipsAccess] with ManyRowModelAccess[Membership]
{
	// IMPLEMENTED	--------------------
	
	override def factory = MembershipFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyMembershipsAccess = ManyMembershipsAccess(condition)
}


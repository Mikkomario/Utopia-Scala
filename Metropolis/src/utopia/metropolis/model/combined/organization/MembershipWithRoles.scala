package utopia.metropolis.model.combined.organization

import utopia.flow.util.Extender
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.Membership

/**
  * Adds role information to a membership
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  */
case class MembershipWithRoles(membership: Membership, roleIds: Set[Int]) extends Extender[MembershipData]
{
	/**
	  * @return Id of this membership
	  */
	def id = membership.id
	
	override def wrapped = membership.data
}

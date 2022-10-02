package utopia.metropolis.model.combined.organization

import utopia.flow.view.template.Extender
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.metropolis.model.stored.organization.{MemberRoleLink, Membership}

/**
  * Adds role information to a membership
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  */
case class MembershipWithRoles(membership: Membership, roleLinks: Set[MemberRoleLink]) extends Extender[MembershipData]
{
	/**
	  * @return Id of this membership
	  */
	def id = membership.id
	
	/**
	  * @return Ids of the roles included in this membership
	  */
	def roleIds = roleLinks.map { _.roleId }
	
	override def wrapped = membership.data
}

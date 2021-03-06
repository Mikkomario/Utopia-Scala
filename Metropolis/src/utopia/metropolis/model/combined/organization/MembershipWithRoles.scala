package utopia.metropolis.model.combined.organization

import utopia.flow.util.Extender
import utopia.metropolis.model.stored.organization.Membership

/**
  * Adds role information to a membership
  * @author Mikko Hilpinen
  * @since 6.5.2020, v1
  */
case class MembershipWithRoles(wrapped: Membership, roleIds: Set[Int]) extends Extender[Membership]

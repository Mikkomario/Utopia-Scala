package utopia.metropolis.model.combined.organization

import utopia.metropolis.model.Extender
import utopia.metropolis.model.enumeration.UserRole
import utopia.metropolis.model.stored.organization.Membership

/**
  * Adds role information to a membership
  * @author Mikko Hilpinen
  * @since 6.5.2020, v2
  */
case class MembershipWithRoles(wrapped: Membership, roles: Set[UserRole]) extends Extender[Membership]

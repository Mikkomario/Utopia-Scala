package utopia.citadel.database.access.single.organization

import utopia.metropolis.model.stored.organization.MemberRoleLink
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual MemberRoles, based on their id
  * @since 2021-10-23
  */
case class DbSingleMemberRoleLink(id: Int) extends UniqueMemberRoleLinkAccess with SingleIntIdModelAccess[MemberRoleLink]


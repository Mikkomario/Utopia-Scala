package utopia.citadel.database.access.single.organization

import utopia.metropolis.model.stored.organization.MemberRole
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual MemberRoles, based on their id
  * @since 2021-10-23
  */
case class DbSingleMemberRole(id: Int) extends UniqueMemberRoleAccess with SingleIntIdModelAccess[MemberRole]


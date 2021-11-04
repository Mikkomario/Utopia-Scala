package utopia.citadel.database.access.single.organization

import utopia.metropolis.model.stored.organization.UserRoleRight
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual UserRoleRights, based on their id
  * @since 2021-10-23
  */
case class DbSingleUserRoleRight(id: Int) 
	extends UniqueUserRoleRightAccess with SingleIntIdModelAccess[UserRoleRight]


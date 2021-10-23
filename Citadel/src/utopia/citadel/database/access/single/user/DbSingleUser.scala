package utopia.citadel.database.access.single.user

import utopia.metropolis.model.stored.user.User
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual Users, based on their id
  * @since 2021-10-23
  */
case class DbSingleUser(id: Int) extends UniqueUserAccess with SingleIntIdModelAccess[User]


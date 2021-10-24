package utopia.exodus.database.access.single.user

import utopia.exodus.model.stored.user.UserPassword
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual UserPasswords, based on their id
  * @since 2021-10-25
  */
case class DbSingleUserPassword(id: Int) 
	extends UniqueUserPasswordAccess with SingleIntIdModelAccess[UserPassword]


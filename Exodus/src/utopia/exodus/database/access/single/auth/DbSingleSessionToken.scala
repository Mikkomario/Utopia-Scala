package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.SessionToken
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual SessionTokens, based on their id
  * @since 2021-10-25
  */
case class DbSingleSessionToken(id: Int) 
	extends UniqueSessionTokenAccess with SingleIntIdModelAccess[SessionToken]


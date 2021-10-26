package utopia.ambassador.database.access.single.token

import utopia.ambassador.model.stored.token.AuthToken
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthTokens, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthToken(id: Int) extends UniqueAuthTokenAccess with SingleIntIdModelAccess[AuthToken]


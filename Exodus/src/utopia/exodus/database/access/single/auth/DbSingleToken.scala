package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.Token
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual tokens, based on their id
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class DbSingleToken(id: Int) extends UniqueTokenAccess with SingleIntIdModelAccess[Token]


package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.TokenType
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual token types, based on their id
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class DbSingleTokenType(id: Int) extends UniqueTokenTypeAccess with SingleIntIdModelAccess[TokenType]


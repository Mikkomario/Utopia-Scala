package utopia.exodus.model.combined.auth

import utopia.exodus.model.stored.auth.{Token, TokenType}

/**
  * Adds type information to a token
  * @author Mikko Hilpinen
  * @since 19.02.2022, v4.0
  */
case class TypedToken(token: Token, tokenType: TokenType) extends TypedTokenLike


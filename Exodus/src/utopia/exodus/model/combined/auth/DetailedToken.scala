package utopia.exodus.model.combined.auth

import utopia.exodus.model.stored.auth.{Token, TokenScopeLink, TokenType}

/**
  * Combines scope and type information to a token
  * @author Mikko Hilpinen
  * @since 19.2.2022, v4.0
  */
case class DetailedToken(token: Token, tokenType: TokenType, scopeLinks: Vector[TokenScopeLink] = Vector())
	extends ScopedTokenLike with TypedTokenLike
{
	// IMPLEMENTED  -----------------------------
	
	override def scopeIds = scopeLinks.map { _.scopeId }.toSet
}

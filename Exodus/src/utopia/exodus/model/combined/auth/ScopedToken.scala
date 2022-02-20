package utopia.exodus.model.combined.auth

import utopia.exodus.model.stored.auth.{Token, TokenScopeLink, TokenType}

/**
  * Includes available scope links (ids) to an access token
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class ScopedToken(token: Token, scopeLinks: Vector[TokenScopeLink]) extends ScopedTokenLike
{
	// OTHER    ------------------------
	
	/**
	  * @param tokenType Type information concerning this token
	  * @return A copy of this token with that information included
	  */
	def withTypeInfo(tokenType: TokenType) = DetailedToken(token, tokenType, scopeLinks)
}


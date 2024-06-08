package utopia.exodus.model.combined.auth

import utopia.exodus.model.stored.auth.{Scope, Token, TokenScopeLink, TokenType}
import utopia.flow.collection.immutable.Empty

/**
  * Combines scope and type information to a token
  * @author Mikko Hilpinen
  * @since 19.2.2022, v4.0
  */
case class DetailedToken(token: Token, tokenType: TokenType, scopeLinks: Seq[TokenScopeLink] = Empty)
	extends ScopedTokenLike with TypedTokenLike
{
	// IMPLEMENTED  --------------------------
	
	override def id = token.id
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param scopePerId A map that contains scope information for each used scope id. Expects all scope that are
	  *                   used in this token to be covered.
	  * @return A copy of this token with appropriate scope link information included
	  */
	def withScopeInfo(scopePerId: Map[Int, Scope]) =
		FullToken(token, tokenType, scopeLinks.map { link => link.withScopeInfo(scopePerId(link.scopeId)) })
}
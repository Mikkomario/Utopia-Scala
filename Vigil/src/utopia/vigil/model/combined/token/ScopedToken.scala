package utopia.vigil.model.combined.token

import utopia.vigil.model.factory.token.TokenFactoryWrapper
import utopia.vigil.model.partial.token.TokenData
import utopia.vigil.model.stored.token.{Token, TokenScope}

object ScopedToken
{
	// OTHER	--------------------
	
	/**
	  * @param token      The token to wrap
	  * @param scopeLinks scope links to attach
	  * @return Combination of the specified token and scope link
	  */
	def apply(token: Token, scopeLinks: Seq[TokenScope]): ScopedToken = apply(token.id, token.data, scopeLinks)
	/**
	  * @param id         ID of this token in the DB
	  * @param data       The wrapped token data
	  * @param scopeLinks scope links to attach
	  * @return A new token with the specified scope link included
	  */
	def apply(id: Int, data: TokenData, scopeLinks: Seq[TokenScope]): ScopedToken = _ScopedToken(id, data, scopeLinks)
	
	
	// NESTED	--------------------
	
	/**
	  * @param id         ID of this token in the DB
	  * @param data       The wrapped token data
	  * @param scopeLinks scope links to attach
	  */
	private case class _ScopedToken(id: Int, data: TokenData, scopeLinks: Seq[TokenScope]) extends ScopedToken
	{
		// IMPLEMENTED	--------------------
		
		override def withId(id: Int) = copy(id = id)
		
		override protected def wrap(factory: TokenData) = copy(data = factory)
	}
}

/**
  * Includes the accessible scope links with a token
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
trait ScopedToken extends Token with TokenFactoryWrapper[TokenData, ScopedToken]
{
	// ABSTRACT	--------------------
	
	/**
	  * Scope links that are attached to this token
	  */
	def scopeLinks: Seq[TokenScope]
}


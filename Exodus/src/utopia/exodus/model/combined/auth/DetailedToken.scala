package utopia.exodus.model.combined.auth

import utopia.exodus.model.partial.auth.{TokenData, TokenTypeData}
import utopia.exodus.model.stored.auth.{Token, TokenScopeLink}
import utopia.flow.util.Extender

/**
  * Combines scope and type information to a token
  * @author Mikko Hilpinen
  * @since 19.2.2022, v4.0
  */
case class DetailedToken(token: Token, tokenType: TokenTypeData, scopeLinks: Vector[TokenScopeLink] = Vector())
	extends Extender[TokenData]
{
	// COMPUTED ---------------------------------
	
	/**
	  * @return Id of this token
	  */
	def id = token.id
	
	/**
	  * @return Ids of the scopes which are accessible using this token
	  */
	def scopeIds = scopeLinks.map { _.scopeId }.toSet
	
	/**
	  * @return A copy of this token without the type information
	  */
	def asScopedToken = ScopedToken(token, scopeLinks)
	
	
	// IMPLEMENTED  -----------------------------
	
	override def wrapped = token.data
}

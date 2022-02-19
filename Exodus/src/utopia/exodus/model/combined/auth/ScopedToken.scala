package utopia.exodus.model.combined.auth

import utopia.exodus.model.partial.auth.TokenData
import utopia.exodus.model.stored.auth.{Token, TokenScopeLink, TokenType}
import utopia.flow.util.Extender

/**
  * Includes available scope links (ids) to an access token
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class ScopedToken(token: Token, scopeLinks: Vector[TokenScopeLink]) extends Extender[TokenData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this token in the database
	  */
	def id = token.id
	
	/**
	  * @return Ids of the scopes which are accessible using this token
	  */
	def scopeIds = scopeLinks.map { _.scopeId }.toSet
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = token.data
	
	
	// OTHER    ------------------------
	
	/**
	  * @param tokenType Type information concerning this token
	  * @return A copy of this token with that information included
	  */
	def withTypeInfo(tokenType: TokenType) = DetailedToken(token, tokenType, scopeLinks)
}


package utopia.exodus.model.combined.auth

import utopia.exodus.model.partial.auth.TokenData
import utopia.exodus.model.stored.auth.{Token, TokenType}
import utopia.flow.view.template.Extender

/**
  * Common trait for tokens which include type information
  * @author Mikko Hilpinen
  * @since 19.2.2022, v4.0
  */
trait TypedTokenLike extends Extender[TokenData]
{
	// ABSTRACT -----------------------------
	
	/**
	  * @return Wrapped token
	  */
	def token: Token
	
	/**
	  * @return Token's type information
	  */
	def tokenType: TokenType
	
	
	// COMPUTED -----------------------------
	
	/**
	  * @return Id of this token
	  */
	def id = token.id
	
	
	// IMPLEMENTED  -------------------------
	
	override def wrapped = token.data
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param scopes Scope information to include
	  * @return A copy of this token with scope information included
	  */
	def withScopes(scopes: Vector[TokenScope]) = FullToken(token, tokenType, scopes)
}

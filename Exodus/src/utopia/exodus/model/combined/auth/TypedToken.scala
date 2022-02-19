package utopia.exodus.model.combined.auth

import utopia.exodus.model.partial.auth.TokenData
import utopia.exodus.model.stored.auth.{Token, TokenType}
import utopia.flow.util.Extender

/**
  * Adds type information to a token
  * @author Mikko Hilpinen
  * @since 19.02.2022, v4.0
  */
case class TypedToken(token: Token, tokenType: TokenType) extends Extender[TokenData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this token in the database
	  */
	def id = token.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = token.data
}


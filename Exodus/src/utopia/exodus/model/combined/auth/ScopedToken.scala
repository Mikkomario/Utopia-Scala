package utopia.exodus.model.combined.auth

import utopia.exodus.model.partial.auth.TokenData
import utopia.exodus.model.stored.auth.{Token, TokenScopeLink}
import utopia.flow.util.Extender

/**
  * Includes available scope links (ids) to an access token
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class ScopedToken(token: Token, scopeLink: Vector[TokenScopeLink]) extends Extender[TokenData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this token in the database
	  */
	def id = token.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = token.data
}


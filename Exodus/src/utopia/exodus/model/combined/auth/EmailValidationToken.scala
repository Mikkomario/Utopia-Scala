package utopia.exodus.model.combined.auth

import utopia.exodus.model.partial.auth.TokenData
import utopia.exodus.model.stored.auth.{EmailValidationAttempt, Token}
import utopia.flow.util.Extender

/**
  * A token with email validation information included
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class EmailValidationToken(token: Token, validation: EmailValidationAttempt) extends Extender[TokenData]
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this token in the database
	  */
	def id = token.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = token.data
}


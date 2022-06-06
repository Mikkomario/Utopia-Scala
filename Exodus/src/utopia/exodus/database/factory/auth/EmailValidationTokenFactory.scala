package utopia.exodus.database.factory.auth

import utopia.exodus.model.combined.auth.EmailValidationToken
import utopia.exodus.model.stored.auth.{EmailValidationAttempt, Token}
import utopia.vault.nosql.factory.row.linked.CombiningFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading email validation tokens from the database
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object EmailValidationTokenFactory 
	extends CombiningFactory[EmailValidationToken, Token, EmailValidationAttempt] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def childFactory = EmailValidationAttemptFactory
	
	override def nonDeprecatedCondition = parentFactory.nonDeprecatedCondition
	
	override def parentFactory = TokenFactory
	
	override def apply(token: Token, validation: EmailValidationAttempt) = EmailValidationToken(token, 
		validation)
}


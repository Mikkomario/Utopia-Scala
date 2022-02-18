package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.model.partial.auth.EmailValidationAttemptData
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading email validation attempt data from the DB
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
object EmailValidationAttemptFactory extends FromValidatedRowModelFactory[EmailValidationAttempt]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = ExodusTables.emailValidationAttempt
	
	override def fromValidatedModel(valid: Model) = 
		EmailValidationAttempt(valid("id").getInt, EmailValidationAttemptData(valid("tokenId").getInt, 
			valid("emailAddress").getString, valid("resendTokenHash").string, valid("sendCount").getInt))
}


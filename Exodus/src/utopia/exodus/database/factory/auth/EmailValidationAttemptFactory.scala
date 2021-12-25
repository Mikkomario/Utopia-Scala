package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.database.model.auth.EmailValidationAttemptModel
import utopia.exodus.model.partial.auth.EmailValidationAttemptData
import utopia.exodus.model.stored.auth.EmailValidationAttempt
import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading EmailValidationAttempt data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object EmailValidationAttemptFactory 
	extends FromValidatedRowModelFactory[EmailValidationAttempt] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def nonDeprecatedCondition = EmailValidationAttemptModel.nonDeprecatedCondition
	
	override def defaultOrdering = None
	
	override def table = ExodusTables.emailValidationAttempt
	
	override def fromValidatedModel(valid: Model) =
		EmailValidationAttempt(valid("id").getInt, EmailValidationAttemptData(valid("purposeId").getInt, 
			valid("email").getString, valid("token").getString, valid("resendToken").getString, 
			valid("expires").getInstant, valid("userId").int, valid("created").getInstant,
			valid("completed").instant))
}


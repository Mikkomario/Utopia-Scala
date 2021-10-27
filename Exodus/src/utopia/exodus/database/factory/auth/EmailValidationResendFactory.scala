package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.model.partial.auth.EmailValidationResendData
import utopia.exodus.model.stored.auth.EmailValidationResend
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading EmailValidationResend data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object EmailValidationResendFactory extends FromValidatedRowModelFactory[EmailValidationResend]
{
	// IMPLEMENTED	--------------------
	
	override def table = ExodusTables.emailValidationResend
	
	override def fromValidatedModel(valid: Model) =
		EmailValidationResend(valid("id").getInt, EmailValidationResendData(valid("validationId").getInt, 
			valid("created").getInstant))
}


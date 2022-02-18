package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.database.model.auth.EmailValidationAttemptModelOld
import utopia.exodus.model.partial.auth.EmailValidationAttemptDataOld
import utopia.exodus.model.stored.auth.EmailValidationAttemptOld
import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading EmailValidationAttempt data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Replaced with a new version", "v4.0")
object EmailValidationAttemptFactoryOld
	extends FromValidatedRowModelFactory[EmailValidationAttemptOld] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def nonDeprecatedCondition = EmailValidationAttemptModelOld.nonDeprecatedCondition
	
	override def defaultOrdering = None
	
	override def table = ExodusTables.emailValidationAttempt
	
	override def fromValidatedModel(valid: Model) =
		EmailValidationAttemptOld(valid("id").getInt, EmailValidationAttemptDataOld(valid("purposeId").getInt,
			valid("email").getString, valid("token").getString, valid("resendToken").getString, 
			valid("expires").getInstant, valid("userId").int, valid("created").getInstant,
			valid("completed").instant))
}


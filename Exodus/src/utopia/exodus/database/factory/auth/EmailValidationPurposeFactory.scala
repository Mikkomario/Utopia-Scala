package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.model.partial.auth.EmailValidationPurposeData
import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.flow.generic.model.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading email validation purpose data from the DB
  * @author Mikko Hilpinen
  * @since 25.10.2021, v4.0
  */
object EmailValidationPurposeFactory extends FromValidatedRowModelFactory[EmailValidationPurpose]
{
	// IMPLEMENTED	--------------------
	
	override def defaultOrdering = None
	
	override def table = ExodusTables.emailValidationPurpose
	
	override def fromValidatedModel(valid: Model) = 
		EmailValidationPurpose(valid("id").getInt, EmailValidationPurposeData(valid("name").getString, 
			valid("created").getInstant))
}


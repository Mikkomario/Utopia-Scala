package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.model.partial.auth.EmailValidationPurposeData
import utopia.exodus.model.stored.auth.EmailValidationPurpose
import utopia.flow.datastructure.immutable.Model
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading EmailValidationPurpose data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object EmailValidationPurposeFactory extends FromValidatedRowModelFactory[EmailValidationPurpose]
{
	// IMPLEMENTED	--------------------
	
	override def table = ExodusTables.emailValidationPurpose
	
	override def defaultOrdering = None
	
	override def fromValidatedModel(valid: Model) =
		EmailValidationPurpose(valid("id").getInt, EmailValidationPurposeData(valid("nameEn").getString, 
			valid("created").getInstant))
}


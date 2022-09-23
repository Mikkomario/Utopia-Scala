package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.database.model.auth.EmailValidatedSessionModel
import utopia.exodus.model.partial.auth.EmailValidatedSessionData
import utopia.exodus.model.stored.auth.EmailValidatedSession
import utopia.flow.collection.value.typeless.Model
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading EmailValidatedSession data from the DB
  * @author Mikko Hilpinen
  * @since 24.11.2021, v3.1
  */
@deprecated("Will be removed in a future release", "v4.0")
object EmailValidatedSessionFactory 
	extends FromValidatedRowModelFactory[EmailValidatedSession] 
		with FromRowFactoryWithTimestamps[EmailValidatedSession] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = EmailValidatedSessionModel.nonDeprecatedCondition
	
	override def table = ExodusTables.emailValidatedSession
	
	override def fromValidatedModel(valid: Model) = 
		EmailValidatedSession(valid("id").getInt, EmailValidatedSessionData(valid("validationId").getInt, 
			valid("token").getString, valid("expires").getInstant, valid("created").getInstant, 
			valid("closedAfter").instant))
}


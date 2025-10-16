package utopia.ambassador.database.factory.service

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.service.AuthServiceSettingsData
import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.TimeUnit.Minute
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading AuthServiceSettings data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object AuthServiceSettingsFactory 
	extends FromValidatedRowModelFactory[AuthServiceSettings] 
		with FromRowFactoryWithTimestamps[AuthServiceSettings]
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def table = AmbassadorTables.authServiceSettings
	
	override def fromValidatedModel(valid: Model) =
		AuthServiceSettings(valid("id").getInt, AuthServiceSettingsData(valid("serviceId").getInt, 
			valid("clientId").getString, valid("clientSecret").getString, 
			valid("authenticationUrl").getString, valid("tokenUrl").getString, 
			valid("redirectUrl").getString, valid("incompleteAuthRedirectUrl").string, 
			valid("defaultCompletionRedirectUrl").string, 
			Minute(valid("preparationTokenDurationMinutes").getLong),
			Minute(valid("redirectTokenDurationMinutes").getLong),
			Minute(valid("incompleteAuthTokenDurationMinutes").getLong),
			Minute(valid("defaultSessionDurationMinutes").getLong),
			valid("created").getInstant))
}


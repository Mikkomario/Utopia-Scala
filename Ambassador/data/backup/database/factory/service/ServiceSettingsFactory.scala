package utopia.ambassador.database.factory.service

import utopia.ambassador.database.AmbassadorTables
import utopia.ambassador.model.partial.service.ServiceSettingsData
import utopia.ambassador.model.stored.service.ServiceSettings
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.flow.generic.ValueUnwraps._
import utopia.flow.time.TimeExtensions._
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading service settings from the DB
  * @author Mikko Hilpinen
  * @since 14.7.2021, v1.0
  */
object ServiceSettingsFactory extends FromValidatedRowModelFactory[ServiceSettings]
	with FromRowFactoryWithTimestamps[ServiceSettings]
{
	override def table = AmbassadorTables.serviceSettings
	
	override def creationTimePropertyName = "created"
	
	override protected def fromValidatedModel(model: Model) = ServiceSettings(model("id"),
		ServiceSettingsData(model("serviceId"), model("clientId"), model("clientSecret"),
			model("authenticationUrl"), model("tokenUrl"), model("redirectUrl"),
			model("incompleteAuthRedirectUrl"), model("defaultCompletionRedirectUrl"),
			model("preparationTokenExpirationMinutes").getInt.minutes,
			model("redirectTokenExpirationMinutes").getInt.minutes,
			model("incompleteAuthTokenExpirationMinutes").getInt.minutes,
			model("defaultSessionExpirationMinutes").getInt.minutes, model("created")))
}

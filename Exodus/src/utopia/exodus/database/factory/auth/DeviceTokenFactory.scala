package utopia.exodus.database.factory.auth

import utopia.exodus.database.ExodusTables
import utopia.exodus.database.model.auth.DeviceTokenModel
import utopia.exodus.model.partial.auth.DeviceTokenData
import utopia.exodus.model.stored.auth.DeviceToken
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading DeviceToken data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DeviceTokenFactory 
	extends FromValidatedRowModelFactory[DeviceToken] with FromRowFactoryWithTimestamps[DeviceToken] 
		with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = DeviceTokenModel.nonDeprecatedCondition
	
	override def table = ExodusTables.deviceToken
	
	override def fromValidatedModel(valid: Model) =
		DeviceToken(valid("id").getInt, DeviceTokenData(valid("deviceId").getInt, valid("userId").getInt, 
			valid("token").getString, valid("created").getInstant, valid("deprecatedAfter").instant))
}


package utopia.citadel.database.factory.device

import utopia.citadel.database.CitadelTables
import utopia.citadel.database.model.device.ClientDeviceUserModel
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.model.immutable.Model
import utopia.metropolis.model.partial.device.ClientDeviceUserData
import utopia.metropolis.model.stored.device.ClientDeviceUser
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading ClientDeviceUser data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
object ClientDeviceUserFactory 
	extends FromValidatedRowModelFactory[ClientDeviceUser] 
		with FromRowFactoryWithTimestamps[ClientDeviceUser] with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = ClientDeviceUserModel.nonDeprecatedCondition
	
	override def table = CitadelTables.clientDeviceUser
	
	override def fromValidatedModel(valid: Model) =
		ClientDeviceUser(valid("id").getInt, ClientDeviceUserData(valid("deviceId").getInt, 
			valid("userId").getInt, valid("created").getInstant, valid("deprecatedAfter").instant))
}


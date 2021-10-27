package utopia.citadel.database.factory.device

import utopia.citadel.database.CitadelTables
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.device.ClientDeviceData
import utopia.metropolis.model.stored.device.ClientDevice
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory

/**
  * Used for reading ClientDevice data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object ClientDeviceFactory extends FromValidatedRowModelFactory[ClientDevice]
{
	// IMPLEMENTED	--------------------
	
	override def table = CitadelTables.clientDevice
	
	override def fromValidatedModel(valid: Model) =
		ClientDevice(valid("id").getInt, ClientDeviceData(valid("creatorId").int, 
			valid("created").getInstant))
}


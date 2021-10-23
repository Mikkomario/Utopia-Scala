package utopia.citadel.database.access.single.device

import utopia.citadel.database.factory.device.ClientDeviceFactory
import utopia.citadel.database.model.description.CitadelDescriptionLinkModel
import utopia.citadel.database.model.device.ClientDeviceModel
import utopia.citadel.model.enumeration.CitadelDescriptionRole.Name
import utopia.metropolis.model.combined.device.DescribedClientDevice
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.partial.device.ClientDeviceData
import utopia.metropolis.model.stored.device.ClientDevice
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual ClientDevices
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbClientDevice extends SingleRowModelAccess[ClientDevice] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ClientDeviceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ClientDeviceFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted ClientDevice instance
	  * @return An access point to that ClientDevice
	  */
	def apply(id: Int) = DbSingleClientDevice(id)
	
	/**
	  * Inserts a new device to the database, including its first name
	  * @param deviceName Name of the device
	  * @param languageId Id of the language the name is written in
	  * @param authorId   Id of the user who added this device
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted device
	  */
	def insert(deviceName: String, languageId: Int, authorId: Int)(implicit connection: Connection) =
	{
		// Inserts a new device first
		val device = model.insert(ClientDeviceData(Some(authorId)))
		// Then inserts a description for the device
		val description = CitadelDescriptionLinkModel.clientDevice.insert(device.id,
			DescriptionData(Name.id, languageId, deviceName, Some(authorId)))
		DescribedClientDevice(device, Set(description))
	}
}


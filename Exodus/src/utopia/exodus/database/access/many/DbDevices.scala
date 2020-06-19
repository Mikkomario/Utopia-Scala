package utopia.exodus.database.access.many

import utopia.exodus.database.model.device.ClientDeviceModel
import utopia.metropolis.model.enumeration.DescriptionRole.Name
import utopia.vault.database.Connection

/**
  * Used for accessing data of multiple devices
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
object DbDevices
{
	private def factory = ClientDeviceModel
	
	/**
	  * Inserts data for a new device
	  * @param deviceName Name of the device
	  * @param languageId Id of the language the name is written in
	  * @param authorId Id of the user who added this device
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted device description, containing new device id
	  */
	def insert(deviceName: String, languageId: Int, authorId: Int)(implicit connection: Connection) =
	{
		// Inserts a new device first
		val newDeviceId = factory.insert(authorId)
		// Then inserts a description for the device
		DbDescriptions.ofDeviceWithId(newDeviceId).update(Name, languageId, authorId, deviceName)
	}
}

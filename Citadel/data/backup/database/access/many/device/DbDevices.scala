package utopia.citadel.database.access.many.device

import utopia.citadel.database.access.single.device.DbDevice
import utopia.vault.database.Connection

/**
  * Used for accessing data of multiple devices
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
  */
object DbDevices
{
	/**
	  * Inserts data for a new device
	  * @param deviceName Name of the device
	  * @param languageId Id of the language the name is written in
	  * @param authorId   Id of the user who added this device
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted device description, containing new device id
	  */
	@deprecated("Please access this method from DbDevice instead", "v1.3")
	def insert(deviceName: String, languageId: Int, authorId: Int)(implicit connection: Connection) =
		DbDevice.insert(deviceName, languageId, authorId)
}

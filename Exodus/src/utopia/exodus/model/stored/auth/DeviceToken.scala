package utopia.exodus.model.stored.auth

import utopia.exodus.database.access.single.auth.DbSingleDeviceToken
import utopia.exodus.model.partial.auth.DeviceTokenData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a DeviceToken that has already been stored in the database
  * @param id id of this DeviceToken in the database
  * @param data Wrapped DeviceToken data
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
case class DeviceToken(id: Int, data: DeviceTokenData) extends StoredModelConvertible[DeviceTokenData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this DeviceToken in the database
	  */
	def access = DbSingleDeviceToken(id)
}


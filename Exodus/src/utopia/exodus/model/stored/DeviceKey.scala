package utopia.exodus.model.stored

import utopia.exodus.model.partial.DeviceKeyData
import utopia.metropolis.model.stored.Stored

/**
  * Represents a device key that has been stored to DB
  * @author Mikko Hilpinen
  * @since 3.5.2020, v2
  */
case class DeviceKey(id: Int, data: DeviceKeyData) extends Stored[DeviceKeyData]

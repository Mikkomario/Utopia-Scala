package utopia.metropolis.model.stored.device

import utopia.metropolis.model.partial.device.ClientDeviceData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

@deprecated("This class will be removed in a future release", "v2.1")
object ClientDevice extends StoredFromModelFactory[ClientDevice, ClientDeviceData]
{
	override def dataFactory = ClientDeviceData
}

/**
  * Represents a ClientDevice that has already been stored in the database
  * @param id id of this ClientDevice in the database
  * @param data Wrapped ClientDevice data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
case class ClientDevice(id: Int, data: ClientDeviceData) extends StoredModelConvertible[ClientDeviceData]


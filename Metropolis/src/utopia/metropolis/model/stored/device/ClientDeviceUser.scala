package utopia.metropolis.model.stored.device

import utopia.metropolis.model.partial.device.ClientDeviceUserData
import utopia.metropolis.model.stored.{StoredFromModelFactory, StoredModelConvertible}

object ClientDeviceUser extends StoredFromModelFactory[ClientDeviceUser, ClientDeviceUserData]
{
	override def dataFactory = ClientDeviceUserData
}

/**
  * Represents a ClientDeviceUser that has already been stored in the database
  * @param id id of this ClientDeviceUser in the database
  * @param data Wrapped ClientDeviceUser data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class ClientDeviceUser(id: Int, data: ClientDeviceUserData) 
	extends StoredModelConvertible[ClientDeviceUserData]


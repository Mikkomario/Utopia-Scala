package utopia.citadel.database.model.device

import java.time.Instant
import utopia.citadel.database.factory.device.ClientDeviceFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.device.ClientDeviceData
import utopia.metropolis.model.stored.device.ClientDevice
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing ClientDeviceModel instances and for inserting ClientDevices to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
object ClientDeviceModel extends DataInserter[ClientDeviceModel, ClientDevice, ClientDeviceData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains ClientDevice creatorId
	  */
	val creatorIdAttName = "creatorId"
	
	/**
	  * Name of the property that contains ClientDevice created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains ClientDevice creatorId
	  */
	def creatorIdColumn = table(creatorIdAttName)
	
	/**
	  * Column that contains ClientDevice created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = ClientDeviceFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: ClientDeviceData) = apply(None, data.creatorId, Some(data.created))
	
	override def complete(id: Value, data: ClientDeviceData) = ClientDevice(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this ClientDevice was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param creatorId Id of the user who added this device, if known
	  * @return A model containing only the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = apply(creatorId = Some(creatorId))
	
	/**
	  * @param id A ClientDevice id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
}

/**
  * Used for interacting with ClientDevices in the database
  * @param id ClientDevice database id
  * @param creatorId Id of the user who added this device, if known
  * @param created Time when this ClientDevice was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
case class ClientDeviceModel(id: Option[Int] = None, creatorId: Option[Int] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[ClientDevice]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ClientDeviceModel.factory
	
	override def valueProperties = 
	{
		import ClientDeviceModel._
		Vector("id" -> id, creatorIdAttName -> creatorId, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param creatorId A new creatorId
	  * @return A new copy of this model with the specified creatorId
	  */
	def withCreatorId(creatorId: Int) = copy(creatorId = Some(creatorId))
}


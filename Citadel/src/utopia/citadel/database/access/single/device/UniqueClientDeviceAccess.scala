package utopia.citadel.database.access.single.device

import java.time.Instant
import utopia.citadel.database.factory.device.ClientDeviceFactory
import utopia.citadel.database.model.device.ClientDeviceModel
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.device.ClientDevice
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct ClientDevices.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
trait UniqueClientDeviceAccess 
	extends SingleRowModelAccess[ClientDevice] 
		with DistinctModelAccess[ClientDevice, Option[ClientDevice], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the user who added this device, if known. None if no instance (or value) was found.
	  */
	def creatorId(implicit connection: Connection) = pullColumn(model.creatorIdColumn).int
	/**
	  * Time when this ClientDevice was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ClientDeviceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ClientDeviceFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted ClientDevice instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any ClientDevice instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the creatorId of the targeted ClientDevice instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any ClientDevice instance was affected
	  */
	def creatorId_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
}


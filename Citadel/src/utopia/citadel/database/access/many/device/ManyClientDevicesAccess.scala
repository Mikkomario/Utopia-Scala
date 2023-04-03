package utopia.citadel.database.access.many.device

import java.time.Instant
import utopia.citadel.database.access.many.description.{DbClientDeviceDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.device.ClientDeviceFactory
import utopia.citadel.database.model.device.ClientDeviceModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.combined.device.DescribedClientDevice
import utopia.metropolis.model.stored.device.ClientDevice
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyClientDevicesAccess
{
	// NESTED	--------------------
	
	private class ManyClientDevicesSubView(override val parent: ManyRowModelAccess[ClientDevice], 
		override val filterCondition: Condition) 
		extends ManyClientDevicesAccess with SubView
}

/**
  * A common trait for access points which target multiple ClientDevices at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
trait ManyClientDevicesAccess 
	extends ManyRowModelAccess[ClientDevice] with ManyDescribedAccess[ClientDevice, DescribedClientDevice]
		with FilterableView[ManyClientDevicesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * creatorIds of the accessible ClientDevices
	  */
	def creatorIds(implicit connection: Connection) = 
		pullColumn(model.creatorIdColumn).flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible ClientDevices
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ClientDeviceModel
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = ClientDeviceFactory
	
	override protected def describedFactory = DescribedClientDevice
	
	override protected def manyDescriptionsAccess = DbClientDeviceDescriptions
	
	override def filter(additionalCondition: Condition): ManyClientDevicesAccess = 
		new ManyClientDevicesAccess.ManyClientDevicesSubView(this, additionalCondition)
	
	override def idOf(item: ClientDevice) = item.id
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted ClientDevice instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any ClientDevice instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the creatorId of the targeted ClientDevice instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any ClientDevice instance was affected
	  */
	def creatorIds_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
}


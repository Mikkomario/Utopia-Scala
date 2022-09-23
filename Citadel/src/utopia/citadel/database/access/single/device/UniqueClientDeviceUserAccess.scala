package utopia.citadel.database.access.single.device

import java.time.Instant
import utopia.citadel.database.factory.device.ClientDeviceUserFactory
import utopia.citadel.database.model.device.ClientDeviceUserModel
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.device.ClientDeviceUser
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct ClientDeviceUsers.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
trait UniqueClientDeviceUserAccess 
	extends SingleRowModelAccess[ClientDeviceUser] 
		with DistinctModelAccess[ClientDeviceUser, Option[ClientDeviceUser], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the device the referenced user is/was using. None if no instance (or value) was found.
	  */
	def deviceId(implicit connection: Connection) = pullColumn(model.deviceIdColumn).int
	
	/**
	  * Id of the user who is/was using this device. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	
	/**
	  * Time when this link was registered (device use started). None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	/**
	  * Time when device use ended. None if no instance (or value) was found.
	  */
	def deprecatedAfter(implicit connection: Connection) = pullColumn(model.deprecatedAfterColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ClientDeviceUserModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ClientDeviceUserFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted ClientDeviceUser instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any ClientDeviceUser instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the deprecatedAfter of the targeted ClientDeviceUser instance(s)
	  * @param newDeprecatedAfter A new deprecatedAfter to assign
	  * @return Whether any ClientDeviceUser instance was affected
	  */
	def deprecatedAfter_=(newDeprecatedAfter: Instant)(implicit connection: Connection) = 
		putColumn(model.deprecatedAfterColumn, newDeprecatedAfter)
	
	/**
	  * Updates the deviceId of the targeted ClientDeviceUser instance(s)
	  * @param newDeviceId A new deviceId to assign
	  * @return Whether any ClientDeviceUser instance was affected
	  */
	def deviceId_=(newDeviceId: Int)(implicit connection: Connection) = 
		putColumn(model.deviceIdColumn, newDeviceId)
	
	/**
	  * Updates the userId of the targeted ClientDeviceUser instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any ClientDeviceUser instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}


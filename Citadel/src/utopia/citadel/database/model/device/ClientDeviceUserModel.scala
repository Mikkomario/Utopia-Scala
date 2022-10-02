package utopia.citadel.database.model.device

import java.time.Instant
import utopia.citadel.database.factory.device.ClientDeviceUserFactory
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.metropolis.model.partial.device.ClientDeviceUserData
import utopia.metropolis.model.stored.device.ClientDeviceUser
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter
import utopia.vault.nosql.storable.deprecation.DeprecatableAfter

/**
  * Used for constructing ClientDeviceUserModel instances and for inserting ClientDeviceUsers to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
object ClientDeviceUserModel 
	extends DataInserter[ClientDeviceUserModel, ClientDeviceUser, ClientDeviceUserData] 
		with DeprecatableAfter[ClientDeviceUserModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains ClientDeviceUser deviceId
	  */
	val deviceIdAttName = "deviceId"
	
	/**
	  * Name of the property that contains ClientDeviceUser userId
	  */
	val userIdAttName = "userId"
	
	/**
	  * Name of the property that contains ClientDeviceUser created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains ClientDeviceUser deprecatedAfter
	  */
	val deprecatedAfterAttName = "deprecatedAfter"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains ClientDeviceUser deviceId
	  */
	def deviceIdColumn = table(deviceIdAttName)
	
	/**
	  * Column that contains ClientDeviceUser userId
	  */
	def userIdColumn = table(userIdAttName)
	
	/**
	  * Column that contains ClientDeviceUser created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains ClientDeviceUser deprecatedAfter
	  */
	def deprecatedAfterColumn = table(deprecatedAfterAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = ClientDeviceUserFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: ClientDeviceUserData) = 
		apply(None, Some(data.deviceId), Some(data.userId), Some(data.created), data.deprecatedAfter)
	
	override def complete(id: Value, data: ClientDeviceUserData) = ClientDeviceUser(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this link was registered (device use started)
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param deprecatedAfter Time when device use ended
	  * @return A model containing only the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = apply(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param deviceId Id of the device the referenced user is/was using
	  * @return A model containing only the specified deviceId
	  */
	def withDeviceId(deviceId: Int) = apply(deviceId = Some(deviceId))
	
	/**
	  * @param id A ClientDeviceUser id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param userId Id of the user who is/was using this device
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with ClientDeviceUsers in the database
  * @param id ClientDeviceUser database id
  * @param deviceId Id of the device the referenced user is/was using
  * @param userId Id of the user who is/was using this device
  * @param created Time when this link was registered (device use started)
  * @param deprecatedAfter Time when device use ended
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
@deprecated("This class will be removed in a future release", "v2.1")
case class ClientDeviceUserModel(id: Option[Int] = None, deviceId: Option[Int] = None, 
	userId: Option[Int] = None, created: Option[Instant] = None, deprecatedAfter: Option[Instant] = None) 
	extends StorableWithFactory[ClientDeviceUser]
{
	// IMPLEMENTED	--------------------
	
	override def factory = ClientDeviceUserModel.factory
	
	override def valueProperties = 
	{
		import ClientDeviceUserModel._
		Vector("id" -> id, deviceIdAttName -> deviceId, userIdAttName -> userId, createdAttName -> created, 
			deprecatedAfterAttName -> deprecatedAfter)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param deprecatedAfter A new deprecatedAfter
	  * @return A new copy of this model with the specified deprecatedAfter
	  */
	def withDeprecatedAfter(deprecatedAfter: Instant) = copy(deprecatedAfter = Some(deprecatedAfter))
	
	/**
	  * @param deviceId A new deviceId
	  * @return A new copy of this model with the specified deviceId
	  */
	def withDeviceId(deviceId: Int) = copy(deviceId = Some(deviceId))
	
	/**
	  * @param userId A new userId
	  * @return A new copy of this model with the specified userId
	  */
	def withUserId(userId: Int) = copy(userId = Some(userId))
}


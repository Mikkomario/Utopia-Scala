package utopia.exodus.database.model.device

import utopia.citadel.database.model.DeprecatableAfter

import java.time.Instant
import utopia.exodus.database.factory.device.DeviceKeyFactory
import utopia.exodus.model.partial.DeviceKeyData
import utopia.exodus.model.stored.DeviceKey
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object DeviceKeyModel extends DeprecatableAfter[DeviceKeyModel]
{
	// COMPUTED ----------------------------------
	
	/**
	  * @return The factory used by this model
	  */
	def factory = DeviceKeyFactory
	
	
	// IMPLEMENTED  ------------------------------
	
	override def table = factory.table
	
	override def withDeprecatedAfter(deprecation: Instant) = apply(deprecatedAfter = Some(deprecation))
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param userId Id of the linked user
	  * @return A model with only user id set
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
	
	/**
	  * @param deviceId Id of targeted device
	  * @return A model with only device id set
	  */
	def withDeviceId(deviceId: Int) = apply(deviceId = Some(deviceId))
	
	/**
	  * @param key Authorization key
	  * @return A model with only key set
	  */
	def withKey(key: String) = apply(key = Some(key))
	
	/**
	  * Inserts a new device authentication key to the database
	  * @param data Data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted device key
	  */
	def insert(data: DeviceKeyData)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(data.userId), Some(data.deviceId), Some(data.key)).insert().getInt
		DeviceKey(newId, data)
	}
}

/**
  * Used for interacting with device authentication keys in DB
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  */
case class DeviceKeyModel(id: Option[Int] = None, userId: Option[Int] = None, deviceId: Option[Int] = None,
						  key: Option[String] = None, deprecatedAfter: Option[Instant] = None)
	extends StorableWithFactory[DeviceKey]
{
	import DeviceKeyModel._
	
	override def factory = DeviceKeyModel.factory
	
	override def valueProperties = Vector("id" -> id, "userId" -> userId, "deviceId" -> deviceId, "key" -> key,
		deprecationAttName -> deprecatedAfter)
}

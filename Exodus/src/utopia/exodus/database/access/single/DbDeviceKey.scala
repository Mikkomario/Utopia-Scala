package utopia.exodus.database.access.single

import utopia.exodus.database.factory.device.DeviceKeyFactory
import utopia.exodus.database.model.device.DeviceKeyModel
import utopia.exodus.model.partial.DeviceKeyData
import utopia.exodus.model.stored.DeviceKey
import utopia.exodus.util.UuidGenerator
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.{SingleIntIdModelAccess, UniqueModelAccess}

/**
  * Used for accessing individual device keys in DB
  * @author Mikko Hilpinen
  * @since 3.5.2020, v1
  */
@deprecated("Replaced with DeviceToken", "v3.0")
object DbDeviceKey extends SingleRowModelAccess[DeviceKey]
{
	// IMPLEMENTED	------------------------------------
	
	override def factory = DeviceKeyFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	
	// COMPUTED	----------------------------------------
	
	private def model = DeviceKeyModel
	
	
	// OTHER	----------------------------------------
	
	/**
	  * @param id Device key id
	  * @return An access point to a device key by that id
	  */
	def apply(id: Int) = new SingleDeviceKeyById(id)
	
	/**
	  * @param deviceId A device id
	  * @return An access point to that device's key
	  */
	def forDeviceWithId(deviceId: Int) = new DbKeyForDevice(deviceId)
	
	/**
	  * @param key Authorization key
	  * @param connection DB Connection (implicit)
	  * @return A device key that matches specified authorization key
	  */
	def matching(key: String)(implicit connection: Connection) = find(model.withKey(key).toCondition)
	
	
	// NESTED	-----------------------------------------
	
	class SingleDeviceKeyById(override val id: Int) extends SingleIntIdModelAccess[DeviceKey]
	{
		override def factory = DbDeviceKey.factory
		
		/**
		  * Invalidates this key so that it can no longer be used for authenticating requests
		  * @param connection DB Connection (implicit)
		  * @return Whether any change was made
		  */
		def invalidate()(implicit connection: Connection) = DbDeviceKey.model.nowDeprecated.updateWhere(
			condition && DbDeviceKey.factory.nonDeprecatedCondition) > 0
	}
	
	class DbKeyForDevice(deviceId: Int) extends UniqueModelAccess[DeviceKey]
	{
		// IMPLEMENTED	-----------------------
		
		override def condition = model.withDeviceId(deviceId).toCondition && factory.nonDeprecatedCondition
		
		override def factory = DeviceKeyFactory
		
		
		// OTHER	---------------------------
		
		/**
		  * Updates this device authentication key
		  * @param userId The user that owns this new key
		  * @param key Key assigned to the user
		  * @param connection DB Connection (implicit)
		  * @return Newly inserted authentication key
		  */
		def update(userId: Int, key: String)(implicit connection: Connection) =
		{
			// Deprecates the old key
			model.nowDeprecated.updateWhere(condition)
			// Inserts a new key
			model.insert(DeviceKeyData(userId, deviceId, key))
		}
		
		/**
		  * Assings this device key to the specified user, invalidating previous users' keys
		  * @param userId If of the user receiving this device key
		  * @param connection DB Connection (implicit)
		  * @return This device key, now belonging to the specified user
		  */
		def assignToUserWithId(userId: Int)(implicit connection: Connection, uuidGenerator: UuidGenerator) =
			update(userId, uuidGenerator.next())
		
		/**
		  * Releases this device authentication key from the specified user, if that user is currently holding this key
		  * @param userId Id of the user this key is released from
		  * @param connection DB Connection (implicit)
		  * @return Whether the user was holding this key (= whether any change was made)
		  */
		def releaseFromUserWithId(userId: Int)(implicit connection: Connection) =
		{
			// Deprecates the device key row (if not already deprecated)
			model.nowDeprecated.updateWhere(mergeCondition(model.withUserId(userId).toCondition)) > 0
		}
	}
}

package utopia.exodus.database.access.single

import java.util.UUID.randomUUID

import utopia.exodus.database.access.many.DbDescriptions
import utopia.exodus.database.factory.device.DeviceKeyFactory
import utopia.exodus.database.model.device.{ClientDeviceModel, DeviceKeyModel}
import utopia.exodus.database.model.user.UserDeviceModel
import utopia.exodus.model.partial.DeviceKeyData
import utopia.exodus.model.stored.DeviceKey
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.enumeration.DescriptionRole.Name
import utopia.vault.database.Connection
import utopia.vault.nosql.access.{SingleModelAccess, UniqueAccess}
import utopia.vault.sql.{Select, Where}

/**
  * Used for accessing and modifying individual devices
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
object DbDevice
{
	// COMPUTED	---------------------------------
	
	private def factory = ClientDeviceModel
	
	
	// OTHER	---------------------------------
	
	/**
	  * @param id A device id
	  * @return An access point to that device's data
	  */
	def apply(id: Int) = new SingleDevice(id)
	
	
	// NESTED	---------------------------------
	
	class SingleDevice(deviceId: Int)
	{
		// COMPUTED	-----------------------------
		
		/**
		  * @param connection DB Connection (implicit)
		  * @return Whether a device with this id exists in the database
		  */
		def isDefined(implicit connection: Connection) = factory.table.containsIndex(deviceId)
		
		/**
		  * @return An access point to this device's authentication key
		  */
		def authenticationKey = DeviceAuthKey
		
		/**
		  * @return An access point to descriptions of this device
		  */
		def descriptions = DbDescriptions.ofDeviceWithId(deviceId)
		
		/**
		  * @param connection Database connection (implicit)
		  * @return Ids of the users who are currently linked to this device
		  */
		def userIds(implicit connection: Connection) =
		{
			val model = UserDeviceModel
			connection(Select(model.table, model.userIdAttName) +
				Where(model.withDeviceId(deviceId).toCondition && model.nonDeprecatedCondition)).rowIntValues
		}
		
		
		// OTHER	-----------------------------
		
		/**
		  * @param languageId Id of targeted language
		  * @param connection DB Connection (implicit)
		  * @return This device's name in specified language
		  */
		def nameInLanguageWithId(languageId: Int)(implicit connection: Connection) =
			DbDescriptions.ofDeviceWithId(deviceId).inLanguageWithId(languageId)(Name)
		
		
		// NESTED	-----------------------------
		
		object DeviceAuthKey extends UniqueAccess[DeviceKey] with SingleModelAccess[DeviceKey]
		{
			// IMPLEMENTED	-----------------------
			
			override def condition = model.withDeviceId(deviceId).toCondition && factory.nonDeprecatedCondition
			
			override def factory = DeviceKeyFactory
			
			
			// COMPUTED	---------------------------
			
			private def model = DeviceKeyModel
			
			
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
			def assignToUserWithId(userId: Int)(implicit connection: Connection) = update(userId,
				randomUUID().toString)
			
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
}

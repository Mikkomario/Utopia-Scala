package utopia.exodus.database.access.single

import java.time.Instant
import utopia.exodus.database.access.many.DbDescriptions
import utopia.exodus.database.factory.device.DeviceKeyFactory
import utopia.exodus.database.model.device.{ClientDeviceModel, DeviceKeyModel}
import utopia.exodus.database.model.user.UserDeviceModel
import utopia.exodus.model.enumeration.StandardDescriptionRoleId
import utopia.exodus.model.partial.DeviceKeyData
import utopia.exodus.model.stored.DeviceKey
import utopia.exodus.util.UuidGenerator
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator.LargerOrEqual
import utopia.vault.nosql.access.UniqueModelAccess
import utopia.vault.sql.{Select, Where}

/**
  * Used for accessing and modifying individual devices
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
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
		
		private def userLinkModel = UserDeviceModel
		
		private def anyUserLinkConnectionCondition = userLinkModel.withDeviceId(deviceId).toCondition
		
		private def activeUserLinkConnectionCondition = anyUserLinkConnectionCondition &&
			userLinkModel.nonDeprecatedCondition
		
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
			val model = userLinkModel
			connection(Select(model.table, model.userIdAttName) + Where(activeUserLinkConnectionCondition)).rowIntValues
		}
		
		
		// OTHER	-----------------------------
		
		/**
		  * Checks whether this device's data has been modified since the specified time threshold
		  * @param threshold A time threshold (inclusive)
		  * @param connection DB Connection (implicit)
		  * @return Whether this devices information has been modified since that time
		  */
		def isModifiedSince(threshold: Instant)(implicit connection: Connection) =
		{
			// Checks for modified device-user links first
			val newLinkCondition = userLinkModel.withCreationTime(threshold).toConditionWithOperator(LargerOrEqual)
			val removedLinkCondition = userLinkModel.withDeprecatedAfter(threshold).toConditionWithOperator(LargerOrEqual)
			if (!userLinkModel.exists(anyUserLinkConnectionCondition && (newLinkCondition || removedLinkCondition)))
			{
				// If not, checks for new descriptions
				DbDescriptions.ofDeviceWithId(deviceId).isModifiedSince(threshold)
			}
			else
				true
		}
		
		/**
		  * @param userId Id of tested user
		  * @param connection DB Connection (implicit)
		  * @return Whether there exists an active link between this device and the specified user
		  */
		def isUsedByUserWithId(userId: Int)(implicit connection: Connection) =
			userLinkModel.exists(activeUserLinkConnectionCondition && userLinkModel.withUserId(userId).toCondition)
		
		/**
		  * @param languageId Id of targeted language
		  * @param connection DB Connection (implicit)
		  * @return This device's name in specified language
		  */
		def nameInLanguageWithId(languageId: Int)(implicit connection: Connection) =
			DbDescription.ofDeviceWithId(deviceId).inLanguageWithId(languageId)
				.forRoleWithId(StandardDescriptionRoleId.name).map { _.description.text }
		
		
		// NESTED	-----------------------------
		
		object DeviceAuthKey extends UniqueModelAccess[DeviceKey]
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
}

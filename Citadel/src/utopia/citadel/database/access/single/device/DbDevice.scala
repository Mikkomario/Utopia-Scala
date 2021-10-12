package utopia.citadel.database.access.single.device

import utopia.citadel.database.access.many.description.DbDescriptions
import utopia.citadel.database.access.single.description.DbDescription
import utopia.citadel.database.model.device.ClientDeviceModel
import utopia.citadel.database.model.user.UserDeviceModel
import utopia.citadel.model.enumeration.CitadelDescriptionRole.Name
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator.LargerOrEqual
import utopia.vault.sql.{Select, Where}

import java.time.Instant

/**
  * Used for accessing and modifying individual devices
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
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
	
	/**
	 * Inserts data for a new device
	 * @param deviceName Name of the device
	 * @param languageId Id of the language the name is written in
	 * @param authorId   Id of the user who added this device
	 * @param connection DB Connection (implicit)
	 * @return Newly inserted device description, containing new device id
	 */
	def insert(deviceName: String, languageId: Int, authorId: Int)(implicit connection: Connection) =
	{
		// Inserts a new device first
		val newDeviceId = factory.insert(authorId)
		// Then inserts a description for the device
		DbDescriptions.ofDeviceWithId(newDeviceId).update(Name.id, languageId, authorId, deviceName)
	}
	
	
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
		  * @param threshold  A time threshold (inclusive)
		  * @param connection DB Connection (implicit)
		  * @return Whether this devices information has been modified since that time
		  */
		def isModifiedSince(threshold: Instant)(implicit connection: Connection) =
		{
			// Checks for modified device-user links first
			val newLinkCondition = userLinkModel.withCreationTime(threshold).toConditionWithOperator(LargerOrEqual)
			val removedLinkCondition = userLinkModel.withDeprecatedAfter(threshold).toConditionWithOperator(LargerOrEqual)
			if (!userLinkModel.exists(anyUserLinkConnectionCondition && (newLinkCondition || removedLinkCondition))) {
				// If not, checks for new descriptions
				DbDescriptions.ofDeviceWithId(deviceId).isModifiedSince(threshold)
			}
			else
				true
		}
		
		/**
		  * @param userId     Id of tested user
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
				.forRoleWithId(Name.id).map { _.description.text }
	}
}

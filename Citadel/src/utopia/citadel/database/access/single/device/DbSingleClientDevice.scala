package utopia.citadel.database.access.single.device

import utopia.citadel.database.access.many.description.DbClientDeviceDescriptions
import utopia.citadel.database.access.many.device.DbClientDeviceUsers
import utopia.citadel.database.access.single.description.{DbClientDeviceDescription, SingleIdDescribedAccess}
import utopia.citadel.database.model.device.ClientDeviceUserModel
import utopia.metropolis.model.combined.device.DescribedClientDevice
import utopia.metropolis.model.stored.device.ClientDevice
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator.LargerOrEqual

import java.time.Instant

/**
  * An access point to individual ClientDevices, based on their id
  * @since 2021-10-23
  */
case class DbSingleClientDevice(id: Int) 
	extends UniqueClientDeviceAccess with SingleIdDescribedAccess[ClientDevice, DescribedClientDevice]
{
	// COMPUTED ------------------------
	
	private def userLinkModel = ClientDeviceUserModel
	
	/**
	  * @return An access point to this device's current user links
	  */
	def userLinks = DbClientDeviceUsers.onDeviceWithId(id)
	/**
	  * @return An access point to this device's current and historical user links
	  */
	def userLinksWithHistory = DbClientDeviceUsers.includingHistory.onDeviceWithId(id)
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Ids of the users who use this device
	  */
	def userIds(implicit connection: Connection) = userLinks.userIds
	
	
	// IMPLEMENTED	--------------------
	
	override protected def describedFactory = DescribedClientDevice
	
	override protected def manyDescriptionsAccess = DbClientDeviceDescriptions
	
	override protected def singleDescriptionAccess = DbClientDeviceDescription
	
	
	// OTHER    ------------------------
	
	/**
	  * @param userId A user id
	  * @return An access point to the possible use link between this device and that user
	  */
	def linkToUserWithId(userId: Int) = DbClientDeviceUser.linkBetween(id, userId)
	
	/**
	  * Checks whether this device's data has been modified since the specified time threshold
	  * @param threshold  A time threshold (inclusive)
	  * @param connection DB Connection (implicit)
	  * @return Whether this devices information has been modified since that time
	  */
	def isModifiedSince(threshold: Instant)(implicit connection: Connection) =
	{
		// Checks for modified device-user links first
		val newLinkCondition = userLinkModel.withCreated(threshold).toConditionWithOperator(LargerOrEqual)
		val removedLinkCondition = userLinkModel.withDeprecatedAfter(threshold).toConditionWithOperator(LargerOrEqual)
		if (!userLinksWithHistory.exists(newLinkCondition || removedLinkCondition))
		{
			// If not, checks for new descriptions
			descriptions.isModifiedSince(threshold)
		}
		else
			true
	}
	
	/**
	  * @param userId     Id of tested user
	  * @param connection DB Connection (implicit)
	  * @return Whether there exists an active link between this device and the specified user
	  */
	def isUsedByUserWithId(userId: Int)(implicit connection: Connection) = linkToUserWithId(userId).nonEmpty
}


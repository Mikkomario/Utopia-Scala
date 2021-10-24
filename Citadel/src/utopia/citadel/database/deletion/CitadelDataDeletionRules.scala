package utopia.citadel.database.deletion

import utopia.citadel.database.CitadelTables
import utopia.citadel.database.model.description.DescriptionModel
import utopia.citadel.database.model.device.ClientDeviceUserModel
import utopia.citadel.database.model.organization.{InvitationModel, MemberRoleLinkModel, MembershipModel}
import utopia.citadel.database.model.user.UserSettingsModel
import utopia.flow.time.TimeExtensions._
import utopia.vault.nosql.storable.deprecation.TimeDeprecatable

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Provides deletion rules for Citadel database data
  * @author Mikko Hilpinen
  * @since 27.6.2021, v1.0
  */
object CitadelDataDeletionRules
{
	// ATTRIBUTES   ------------------------------
	
	/**
	  * The length of time that historical data is kept in the database by default
	  */
	val defaultHistoryDuration = 30.days
	
	
	// COMPUTED -----------------------------------
	
	/**
	  * @return Tables that should be targeted with the "clear unreferenced data" -operation,
	  *         namely: client device
	  */
	def unreferencedDeletionTables = Vector(CitadelTables.clientDevice)
	
	/**
	  * @return A set of deletion rules where all expired / deprecated items are deleted after 30 days
	  */
	def default = custom()
	
	/**
	  * @return A set of deletion rules that doesn't keep expired / deprecated items in the database for long
	  */
	def noHistory = sameForAll(Duration.Zero)
	
	
	// OTHER    -----------------------------------
	
	/**
	  * Creates a set of deletion rules with custom history durations
	  * @param userSettings Duration to keep old user settings (default = 30 days)
	  * @param deviceUsers  Duration to keep old device users (default = 30 days)
	  * @param memberships  Duration to keep ended memberships (default = 30 days)
	  * @param memberRole   Duration to keep old member roles (default = 30 days)
	  * @param invitation   Duration to keep expired invitations (default = 30 days)
	  * @param description  Duration to keep deprecated descriptions (default = 30 days)
	  * @return A set of deletion rules for applicable tables
	  */
	def custom(userSettings: Duration = defaultHistoryDuration,
	           deviceUsers: Duration = defaultHistoryDuration,
	           memberships: Duration = defaultHistoryDuration,
	           memberRole: Duration = defaultHistoryDuration,
	           invitation: Duration = defaultHistoryDuration,
	           description: Duration = defaultHistoryDuration) =
	{
		Vector(
			deprecation(UserSettingsModel, userSettings),
			deprecation(ClientDeviceUserModel, deviceUsers),
			deprecation(MembershipModel, memberships),
			deprecation(MemberRoleLinkModel, memberRole),
			deprecation(InvitationModel, invitation),
			deprecation(DescriptionModel, description)
		).flatten
	}
	
	/**
	  * Creates a set of deletion rules with a single history duration that applies to all expiring and
	  * deprecatable items
	  * @param historyDuration The amount of time deprecated / expired data is kept in the database
	  * @return A new set of deletion rules
	  */
	def sameForAll(historyDuration: FiniteDuration) =
		custom(historyDuration, historyDuration, historyDuration, historyDuration, historyDuration,
			historyDuration)
	
	/**
	  * Creates a rule around (null) deprecation
	  * @param model Model factory class
	  * @param duration History duration
	  * @return A deletion rule. None if history duration is infinite.
	  */
	def deprecation(model: TimeDeprecatable, duration: Duration) =
		duration.finite.map(model.deletionAfterDeprecation)
}

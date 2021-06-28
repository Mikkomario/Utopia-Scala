package utopia.citadel.database.deletion

import utopia.citadel.database.Tables
import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.organization.{InvitationModel, MemberRoleModel, MembershipModel}
import utopia.citadel.database.model.user.{UserDeviceModel, UserSettingsModel}
import utopia.citadel.database.model.{Expiring, NullDeprecatable}
import utopia.flow.time.TimeExtensions._

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
	
	// TODO: Following need custom implementations
	/*
		- organization
		- cancelled organization deletion
	 */
	
	
	// COMPUTED -----------------------------------
	
	/**
	  * @return Tables that should be targeted with the "clear unreferenced data" -operation,
	  *         namely: description and client device
	  */
	def unreferencedDeletionTables = Vector(Tables.description, Tables.clientDevice)
	
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
		val base = Vector(
			deprecation(UserSettingsModel, userSettings),
			deprecation(UserDeviceModel, deviceUsers),
			deprecation(MembershipModel, memberships),
			deprecation(MemberRoleModel, memberRole),
			expiration(InvitationModel, invitation),
		).flatten
		description.finite match {
			case Some(descriptionHistoryDuration) =>
				base ++ DescriptionLinkFactory.defaultImplementations
					.map { _.modelFactory.deletionAfterDeprecation(descriptionHistoryDuration) }
			case None => base
		}
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
	
	private def deprecation(model: NullDeprecatable[_], duration: Duration) =
		duration.finite.map(model.deletionAfterDeprecation)
	
	private def expiration(model: Expiring, duration: Duration) =
		duration.finite.map(model.deletionAfterExpiration)
}

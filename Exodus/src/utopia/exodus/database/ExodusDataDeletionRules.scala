package utopia.exodus.database

import utopia.citadel.database.deletion.CitadelDataDeletionRules
import CitadelDataDeletionRules.defaultHistoryDuration
import CitadelDataDeletionRules.deprecation
import utopia.exodus.database.model.auth.{DeviceTokenModel, EmailValidationAttemptModelOld, SessionTokenModel}
import utopia.flow.time.TimeExtensions._
import utopia.vault.model.immutable.DataDeletionRule

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * An object used for acquiring data deletion rules concerning Exodus-specific tables
  * @author Mikko Hilpinen
  * @since 29.6.2021, v2.0
  */
object ExodusDataDeletionRules
{
	// COMPUTED ---------------------------------
	
	/**
	  * @return Deletion rules for Exodus-specific resources where all items are deleted after 30 days of
	  *         deprecation / expiration
	  */
	def default = custom()
	/**
	  * @return Deletion rules that don't keep historical records of Exodus-specific resources
	  */
	def noHistory = sameForAll(Duration.Zero)
	
	
	// OTHER    ---------------------------------
	
	/**
	  * Acquires only the rules that are Exodus-specific
	  * @param session User session history duration (default = 30 days)
	  * @param deviceKey Device auth key history duration (default = 30 days)
	  * @param emailValidation Email validation history duration (default = 30 days)
	  * @return Data deletion rules for Exodus-specific tables/resources
	  */
	def custom(session: Duration = defaultHistoryDuration, deviceKey: Duration = defaultHistoryDuration,
	               emailValidation: Duration = defaultHistoryDuration) =
		Vector(
			session.finite.map { duration => DataDeletionRule(SessionTokenModel.table,
				SessionTokenModel.expiresAttName, duration) },
			deprecation(DeviceTokenModel, deviceKey),
			deprecation(EmailValidationAttemptModelOld, emailValidation)).flatten
	
	/**
	  * Uses the same history duration for all of the tables
	  * @param historyDuration History duration to use for all Exodus-specific resources
	  * @return Exodus-specific data deletion rules
	  */
	def sameForAll(historyDuration: FiniteDuration) =
		custom(historyDuration, historyDuration, historyDuration)
}

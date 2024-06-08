package utopia.exodus.database

import utopia.citadel.database.deletion.CitadelDataDeletionRules
import CitadelDataDeletionRules.defaultHistoryDuration
import utopia.exodus.database.model.auth.TokenModel
import utopia.flow.collection.immutable.Empty
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
	  * @param token Authorization token history duration (default = 30 days)
	  * @return Data deletion rules for Exodus-specific tables/resources
	  */
	def custom(token: Duration = defaultHistoryDuration) = token.finite match {
		case Some(duration) =>
			val tokenModel = TokenModel
			Vector(
				DataDeletionRule(tokenModel.table, tokenModel.expiresAttName, duration),
				DataDeletionRule(tokenModel.table, tokenModel.deprecatedAfterAttName, duration)
			)
		case None => Empty
	}
	
	/**
	  * Uses the same history duration for all of the tables
	  * @param historyDuration History duration to use for all Exodus-specific resources
	  * @return Exodus-specific data deletion rules
	  */
	def sameForAll(historyDuration: FiniteDuration) = custom(historyDuration)
}

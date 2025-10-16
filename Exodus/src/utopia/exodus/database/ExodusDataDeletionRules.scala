package utopia.exodus.database

import utopia.citadel.database.deletion.CitadelDataDeletionRules.defaultHistoryDuration
import utopia.exodus.database.model.auth.TokenModel
import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.time.Duration
import utopia.vault.model.immutable.DataDeletionRule

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
	def noHistory = sameForAll(Duration.zero)
	
	
	// OTHER    ---------------------------------
	
	/**
	  * Acquires only the rules that are Exodus-specific
	  * @param token Authorization token history duration (default = 30 days)
	  * @return Data deletion rules for Exodus-specific tables/resources
	  */
	def custom(token: Duration = defaultHistoryDuration) = token.ifFinite match {
		case Some(duration) =>
			val tokenModel = TokenModel
			Pair(
				DataDeletionRule(tokenModel.expiresColumn, duration),
				DataDeletionRule(tokenModel.deprecatedAfterColumn, duration)
			)
		case None => Empty
	}
	
	/**
	  * Uses the same history duration for all of the tables
	  * @param historyDuration History duration to use for all Exodus-specific resources
	  * @return Exodus-specific data deletion rules
	  */
	def sameForAll(historyDuration: Duration) = custom(historyDuration)
}

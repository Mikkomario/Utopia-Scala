package utopia.citadel.database.model

import utopia.flow.time.Now
import utopia.vault.model.immutable.{DataDeletionRule, Table}
import utopia.vault.nosql.factory.Deprecatable
import utopia.vault.sql.SqlExtensions._

import scala.concurrent.duration.FiniteDuration

/**
 * A common trait for model factories that support deprecation by utilizing a
 * (not null) timestamp column that contains the (preset) expiration time of an item
 * @author Mikko Hilpinen
 * @since 27.6.2021, v1.0
 */
trait Expiring extends Deprecatable
{
	// ABSTRACT --------------------------------
	
	/**
	 * @return Name of the property that contains item expiration time, which may be in the future
	 */
	def expirationAttName: String
	
	/**
	 * @return Table used by this model
	 */
	def table: Table
	
	
	// COMPUTED --------------------------------
	
	/**
	 * @return Column that contains item expiration time, which may be in the future
	 */
	def expirationColumn = table(expirationAttName)
	
	/**
	 * @return A deletion rule that targets all items that have been expired
	 */
	def immediateDeletionRule = DataDeletionRule.onArrivalOf(table, expirationAttName)
	
	
	// IMPLEMENTED  ----------------------------
	
	override def nonDeprecatedCondition = expirationColumn <= Now.toValue
	
	
	// OTHER    --------------------------------
	
	/**
	 * @param historyDuration How long expired items should be kept in the database
	 * @return A rule that targets items that have been expired long enough ago
	 */
	def deletionAfterExpiration(historyDuration: FiniteDuration) =
		DataDeletionRule(table, expirationAttName, historyDuration)
}

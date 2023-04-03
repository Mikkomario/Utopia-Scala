package utopia.citadel.database.model

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.model.immutable.{DataDeletionRule, Table}
import utopia.vault.nosql.template.Deprecatable

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

/**
 * Common trait for deprecatable model factories that use a deprecation time column
 * @author Mikko Hilpinen
 * @since 3.7.2021, v1.0
 */
@deprecated("This trait is now available in Vault", "v1.2")
trait TimeDeprecatable extends Deprecatable
{
	// ABSTRACT ----------------------------------
	
	/**
	 * @return The table used by this class
	 */
	def table: Table
	/**
	 * @return Name of the property that contains item deprecation time
	 */
	def deprecationAttName: String
	
	
	// COMPUTED -----------------------------------
	
	/**
	 * @return Column that contains item deprecation timestamp
	 */
	def deprecationColumn = table(deprecationAttName)
	
	/**
	 * @return A deletion rule that deletes deprecated items as soon as possible
	 */
	def immediateDeletionRule = DataDeletionRule.onArrivalOf(table, deprecationAttName)
	
	
	// OTHER    -----------------------------------
	
	/**
	 * @param threshold A time threshold
	 * @return A condition that returns items that were deprecated after the specified time threshold
	 */
	def deprecatedAfterCondition(threshold: Instant) = deprecationColumn > threshold
	/**
	 * @param threshold A time threshold
	 * @return A condition that returns items that were deprecated before the specified time threshold
	 */
	def deprecatedBeforeCondition(threshold: Instant) = deprecationColumn < threshold
	
	/**
	 * @param historyDuration Duration how long the item is kept in the database after deprecation
	 * @return A new deletion rule that applies to this model type
	 */
	def deletionAfterDeprecation(historyDuration: FiniteDuration) =
		DataDeletionRule(table, deprecationAttName, historyDuration)
}

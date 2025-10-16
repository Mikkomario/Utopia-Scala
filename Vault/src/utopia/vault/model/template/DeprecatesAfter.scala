package utopia.vault.model.template

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.model.immutable.{DataDeletionRule, TableColumn}
import utopia.vault.sql.Condition

import java.time.Instant
import utopia.flow.time.Duration

/**
  * Common trait for interfaces which use a timestamp-based deprecation column
  * to distinguish between active and deprecated items.
  *
  * This trait doesn't determine whether that column is nullable or not,
  * i.e. whether it is always specified or only specified upon deprecation.
  *
  * @author Mikko Hilpinen
  * @since 03.08.2025, v2.0
  */
trait DeprecatesAfter extends Deprecates
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Column which contains the deprecation timestamp
	  */
	def deprecationColumn: TableColumn
	
	
	// COMPUTED ------------------------
	
	/**
	  * @return A deletion rule that deletes deprecated items as soon as possible
	  */
	def immediateDeletionRule = DataDeletionRule.at(deprecationColumn)
	
	
	// IMPLEMENTED  --------------------
	
	override def activeCondition: Condition = deprecationColumn > Now
	override def deprecatedCondition = deprecationColumn <= Now
	
	
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
	def deletionAfterDeprecation(historyDuration: Duration) = DataDeletionRule(deprecationColumn, historyDuration)
}

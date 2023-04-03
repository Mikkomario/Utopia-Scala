package utopia.vault.nosql.view

import utopia.vault.database.Connection
import utopia.vault.nosql.storable.deprecation.TimeDeprecatable

import java.time.Instant

/**
  * Common trait for access points / views that target time-based deprecatable items
  * @author Mikko Hilpinen
  * @since 3.4.2023, v1.15.1
  */
trait TimeDeprecatableView[+Sub] extends FilterableView[Sub]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Model used for interacting with the DB and for building conditions
	  */
	protected def model: TimeDeprecatable
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return Access to non-deprecated items
	  */
	def nonDeprecated = filter(model.nonDeprecatedCondition)
	
	
	// OTHER    ----------------------
	
	/**
	  * @param threshold A time threshold
	  * @return Access to items that were deprecated after the specified time threshold
	  */
	def deprecatedAfter(threshold: Instant) = filter(model.deprecatedAfterCondition(threshold))
	/**
	  * @param threshold A time threshold
	  * @return Access to items that were deprecated before the specified time threshold
	  */
	def deprecatedBefore(threshold: Instant) = filter(model.deprecatedBeforeCondition(threshold))
	
	/**
	  * @param threshold A time threshold
	  * @param c Implicit DB Connection
	  * @return Whether any accessible item was deprecated after the specified time threshold
	  */
	def wasDeprecatedAfter(threshold: Instant)(implicit c: Connection) =
		exists(model.deprecatedAfterCondition(threshold))
}

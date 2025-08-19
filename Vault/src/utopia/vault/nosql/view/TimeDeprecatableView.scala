package utopia.vault.nosql.view

import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.model.template.DeprecatesAfter
import utopia.vault.sql.{Update, Where}

import java.time.Instant

/**
  * Common trait for access points / views that target time-based deprecatable items
  * @author Mikko Hilpinen
  * @since 3.4.2023, v1.15.1
  */
trait TimeDeprecatableView[+Sub] extends DeprecatableView[Sub]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return Model used for interacting with the DB and for building conditions
	  */
	override def model: DeprecatesAfter
	
	
	// COMPUTED ----------------------
	
	@deprecated("Renamed to .active", "v1.22")
	def nonDeprecated = active
	
	
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
	
	/**
	 * Deprecates all accessible items
	 * @param c Implicit database connection
	 * @return Whether any row was targeted
	 */
	def deprecate()(implicit c: Connection) =
		c(Update(target, model.deprecationColumn, Now.toValue) + accessCondition.map(Where.apply)).updatedRows
}

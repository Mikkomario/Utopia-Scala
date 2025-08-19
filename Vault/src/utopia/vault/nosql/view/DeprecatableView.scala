package utopia.vault.nosql.view

import utopia.vault.model.template.Deprecates

/**
 * Common trait for views which distinguish between active and deprecated items
 *
 * @author Mikko Hilpinen
 * @since 09.07.2025, v1.22
 */
trait DeprecatableView[+Repr] extends FilterableView[Repr]
{
	// ABSTRACT -----------------------
	
	/**
	 * @return Model used for interacting with the DB and for building conditions
	 */
	def model: Deprecates
	
	
	// COMPUTED ----------------------
	
	/**
	 * @return Access to non-deprecated items
	 */
	def active = filter(model.activeCondition)
	/**
	 * @return Access limited to deprecated items
	 */
	def historical = filter(model.deprecatedCondition)
	
	
	// OTHER    ----------------------
	
	/**
	 * @param condition A condition that, if met, causes only active instances to be accessed
	 * @return If 'condition' is met, a copy of this access which only includes active instances.
	 *         If not met, this access.
	 */
	def limitedToActiveIf(condition: Boolean) = if (condition) active else self
	/**
	 * @param condition A condition that, if met, causes only historical instances to be accessed
	 * @return If 'condition' is met, a copy of this access which only includes historical instances.
	 *         If not met, this access.
	 */
	def limitedToHistoricalIf(condition: Boolean) = if (condition) historical else self
}

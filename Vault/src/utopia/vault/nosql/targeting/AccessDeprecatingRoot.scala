package utopia.vault.nosql.targeting

import utopia.vault.nosql.targeting.many.AccessManyDeprecatingRoot
import utopia.vault.nosql.targeting.one.AccessOneDeprecatingRoot
import utopia.vault.nosql.view.DeprecatableView

object AccessDeprecatingRoot
{
	/**
	 * @return Constructors for creating access into individual items
	 */
	def one = AccessOneDeprecatingRoot
	/**
	 * @return Constructors for creating access into multiple items at once
	 */
	def many = AccessManyDeprecatingRoot
}

/**
 * Common trait for root level access points that can target active and/or historical items separately or together.
 *
 * @author Mikko Hilpinen
 * @since 08.08.2025, v2.0
 */
trait AccessDeprecatingRoot[+A <: DeprecatableView[A]]
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Access to both active and historical items
	 */
	def all: A
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Access limited to active items
	 */
	def active = all.active
	/**
	 * @return Access limited to historical items
	 */
	def historical = all.historical
}

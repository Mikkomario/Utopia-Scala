package utopia.vault.nosql.storable.deprecation

import utopia.vault.model.immutable.Storable

object DeprecatableAfter
{
	/**
	 * Name of the property that contains item deprecation timestamp
	 */
	val deprecationAttName = "deprecatedAfter"
}

/**
 * A common trait for model factories that support deprecation by utilizing a
 * "deprecatedAfter" column that is null by default and
 * is set to current timestamp when an item gets deprecated
 * @author Mikko Hilpinen
 * @since 26.9.2021, v1.10
 */
trait DeprecatableAfter[+M <: Storable] extends NullDeprecatable[M]
{
	// IMPLEMENTED  ----------------------------
	
	def deprecationAttName = DeprecatableAfter.deprecationAttName
}

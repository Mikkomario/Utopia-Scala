package utopia.citadel.database.model

import utopia.vault.model.immutable.Storable

@deprecated("This trait is now available in Vault", "v1.2")
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
 * @since 27.6.2021, v1.0
 */
@deprecated("This trait is now available in Vault", "v1.2")
trait DeprecatableAfter[+M <: Storable] extends NullDeprecatable[M]
{
	// IMPLEMENTED  ----------------------------
	
	def deprecationAttName = DeprecatableAfter.deprecationAttName
}

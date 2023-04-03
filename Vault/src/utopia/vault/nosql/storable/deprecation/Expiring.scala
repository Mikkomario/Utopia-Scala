package utopia.vault.nosql.storable.deprecation

import utopia.flow.time.Now

/**
 * A common trait for model factories that support deprecation by utilizing a
 * (not null) timestamp column that contains the (preset) expiration time of an item
 * @author Mikko Hilpinen
 * @since 26.9.2021, v1.0
 */
trait Expiring extends TimeDeprecatable
{
	// IMPLEMENTED  ----------------------------
	
	override def nonDeprecatedCondition = deprecationColumn > Now.toValue
}

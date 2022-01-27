package utopia.citadel.database.model

import utopia.flow.time.Now
import utopia.vault.sql.SqlExtensions._

/**
 * A common trait for model factories that support deprecation by utilizing a
 * (not null) timestamp column that contains the (preset) expiration time of an item
 * @author Mikko Hilpinen
 * @since 27.6.2021, v1.0
 */
@deprecated("This trait is now available in Vault", "v1.2")
trait Expiring extends TimeDeprecatable
{
	// IMPLEMENTED  ----------------------------
	
	override def nonDeprecatedCondition = deprecationColumn > Now.toValue
}

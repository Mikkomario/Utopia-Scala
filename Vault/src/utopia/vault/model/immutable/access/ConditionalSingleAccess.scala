package utopia.vault.model.immutable.access

import utopia.vault.database.Connection

/**
 * Provides access to 0-1 items based on a search condition
 * @author Mikko Hilpinen
 * @since 6.10.2019, v1.3.1+
 */
@deprecated("Replaced with utopia.vault.nosql.access.single.model.SingleModelAccess", "v1.4")
trait ConditionalSingleAccess[+A] extends ConditionalAccess[A]
{
	/**
	 * Reads model data from table
	 * @param connection Database connection (implicit)
	 * @return Model read for this id
	 */
	def get(implicit connection: Connection) = factory.find(condition)
}

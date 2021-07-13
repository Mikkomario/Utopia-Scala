package utopia.vault.model.immutable.access

import utopia.vault.database.Connection
import utopia.vault.sql.Condition

/**
 * Used for reading multiple ids from database
 * @author Mikko Hilpinen
 * @since 30.7.2019, v1.3+
 */
@deprecated("Replaced with utopia.vault.nosql.access.many.id.ManyIdAccess", "v1.4")
trait ManyIdAccess[+I] extends IdAccess[I]
{
	// COMPUTED	-------------------
	
	/**
	 * @param connection Database connection (implicit)
	 * @return All indices in targeted table
	 */
	def all(implicit connection: Connection) = table.allIndices.map(valueToId)
	
	/**
	 * Reads multiple indices from database
	 * @param condition A search condition
	 * @param connection Database connection
	 * @return Found indices that match the search condition
	 */
	def apply(condition: Condition)(implicit connection: Connection) = table.indices(condition).map(valueToId)
}
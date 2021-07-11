package utopia.vault.model.immutable.access

import utopia.vault.database.Connection
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.{Condition, Delete, Exists, Where}

/**
 * Provides access to a single (or multiple) items in database
 * @author Mikko Hilpinen
 * @since 5.10.2019, v1.3.1+
 */
@deprecated("Replaced with utopia.vault.nosql.access.template.FilterableAccess", "v1.4")
trait ConditionalAccess[+A]
{
	// ABSTRACT	------------------
	
	/**
	 * @return Condition of this access
	 */
	def condition: Condition
	
	/**
	 * @return Factory used by this access
	 */
	def factory: FromResultFactory[A]
	
	
	// OTHER	------------------
	
	/**
	 * Checks whether this index is valid (exists in database)
	 * @param connection Database connection (implicit)
	 * @return Whether this id exists in the database
	 */
	def exists(implicit connection: Connection) = Exists(factory.table, condition)
	
	/**
	 * Deletes this id from database
	 * @param connection Database connection (implicit)
	 * @return Whether any rows were deleted
	 */
	def delete(implicit connection: Connection) = connection(Delete(factory.table) + Where(condition)).updatedRows
}

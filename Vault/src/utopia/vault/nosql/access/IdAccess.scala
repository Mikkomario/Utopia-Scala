package utopia.vault.nosql.access

import utopia.flow.datastructure.immutable.Value
import utopia.vault.sql.SqlTarget

/**
 * A common trait for access points that are used for accessing row ids (primary keys)
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait IdAccess[+ID, +A] extends IndexedAccess[A]
{
	// ABSTRACT	-----------------------
	
	/**
	 * @return The whole data selection target, which may include multiple tables (ids are collected from the primary
	 *         table, however)
	 */
	def target: SqlTarget
	
	/**
	 * Converts a value to an id
	 * @param value Value to convert
	 * @return An id from the value. May be None (if value is empty, for example)
	 */
	def valueToId(value: Value): Option[ID]
}

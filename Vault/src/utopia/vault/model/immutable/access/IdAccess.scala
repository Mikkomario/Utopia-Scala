package utopia.vault.model.immutable.access

import utopia.flow.datastructure.immutable.Value
import utopia.vault.model.immutable.Table

/**
 * Used for accessing row ids for specific conditions
 * @author Mikko Hilpinen
 * @since 30.7.2019, v1.3+
 */
@deprecated("Replaced with utopia.vault.nosql.access.IdAccess", "v1.4")
trait IdAccess[+I]
{
	// ABSTRACT	------------------
	
	/**
	 * @return The table from which ids are read
	 */
	def table: Table
	
	/**
	 * @return The index column used by this access
	 */
	def index = table.primaryColumn.get
	
	/**
	 * Converts a value to specified index type
	 * @param value Value to convert
	 * @return Converted value
	 */
	protected def valueToId(value: Value): I
}
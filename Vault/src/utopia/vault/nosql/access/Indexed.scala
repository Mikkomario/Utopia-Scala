package utopia.vault.nosql.access

import utopia.vault.model.immutable.Table

/**
 * A common trait for access points that use indexed tables
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait Indexed
{
	// ABSTRACT -----------------------
	
	/**
	 * @return Table used by this class
	 */
	def table: Table
	
	
	// COMPUTED	-----------------------
	
	/**
	 * @return The index column in the primary table
	 */
	def index = table.primaryColumn.get
}

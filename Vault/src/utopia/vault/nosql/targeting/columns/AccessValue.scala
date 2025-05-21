package utopia.vault.nosql.targeting.columns

import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn

/**
  * Common trait for interfaces that provide access to individual values of multiple columns
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
trait AccessValue
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return An access point used for pulling column data
	  */
	def access: AccessColumn
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param column Targeted column
	  * @return Access to constructors of factories for that column
	  */
	def apply(column: Column) = AccessColumnValue(access, column)
}

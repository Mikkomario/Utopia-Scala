package utopia.vault.nosql.targeting.columns

import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessManyColumns

/**
  * Common trait for custom interfaces that provide access to parsed column values based on a targeted access point
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
trait AccessValues
{
	// ABSTRACT -----------------------
	
	/**
	  * @return The used access point, which limits the access to the targeted area & tables
	  */
	protected def access: AccessManyColumns
	
	
	// OTHER    ----------------------
	
	/**
	  * @param column Targeted column
	  * @return Factory for constructing an access point to the specified column
	  */
	def apply(column: Column) = AccessColumnValues(access, column)
}

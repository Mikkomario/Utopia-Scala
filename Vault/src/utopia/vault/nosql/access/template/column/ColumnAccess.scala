package utopia.vault.nosql.access.template.column

import utopia.flow.datastructure.immutable.Value
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.access.template.Access

/**
  * A common trait for access points that are used for accessing values of a certain table column
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  * @tparam V Type of the read column values (when including empty value cases)
  * @tparam A The format in which read results are returned
  */
trait ColumnAccess[+V, +A] extends Access[A]
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return The column being read
	  */
	def column: Column
	
	/**
	  * Converts a value to a read item
	  * @param value A read column value
	  * @return The value in the correct type
	  */
	def parseValue(value: Value): V
}

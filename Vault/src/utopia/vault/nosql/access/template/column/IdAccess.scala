package utopia.vault.nosql.access.template.column

import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that are used for accessing row ids (primary keys)
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait IdAccess[+ID, +A] extends ColumnAccess[Option[ID], A] with Indexed
{
	// ABSTRACT	-----------------------
	
	/**
	  * Converts a value to an id
	  * @param value Value to convert
	  * @return An id from the value. May be None (if value is empty, for example)
	  */
	def valueToId(value: Value): Option[ID]
	
	
	// IMPLEMENTED  -------------------
	
	override def column = index
	
	override def parseValue(value: Value) = valueToId(value)
}

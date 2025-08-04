package utopia.vault.nosql.targeting.columns

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.template.Indexed

object AccessColumns
{
	/**
	  * A type alias for column access points which target one row at a time
	  */
	type AccessColumn = AccessColumns[Value]
}

/**
  * Common trait for interfaces which provide access to database column contents
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.21
  */
trait AccessColumns[+V] extends Indexed
{
	// ABSTRACT ---------------------------
	
	/**
	  * @param column Targeted column
	  * @param distinct Whether the targeted column values should be distinct from each other (default = false)
	  * @return Data from the targeted column within the targeted data
	  */
	def apply(column: Column, distinct: Boolean)(implicit connection: Connection): V
	/**
	  * @param columns Targeted columns
	  * @param connection Implicit DB connection
	  * @return Data of the targeted columns from the targeted item(s)
	  */
	def apply(columns: Seq[Column])(implicit connection: Connection): Seq[V]
	
	/**
	  * Updates the column value(s) of the targeted item(s)
	  * @param column Targeted column
	  * @param value Assigned value
	  * @param connection Implicit DB connection
	  * @return Whether any item was targeted
	  */
	def update(column: Column, value: Value)(implicit connection: Connection): Boolean
	/**
	  * Updates column values of the targeted items
	  * @param assignments Column value assignments, where each entry contains:
	  *                         1. Modified column
	  *                         1. Assigned value
	  * @param connection Implicit DB connection
	  * @return Whether any row / item was targeted
	  */
	def update(assignments: IterableOnce[(Column, Value)])(implicit connection: Connection): Boolean
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param column Targeted column
	  * @return Data from the targeted column within the targeted data
	  */
	def apply(column: Column)(implicit connection: Connection): V = apply(column, distinct = false)
	/**
	  * @param firstColumn First targeted column
	  * @param secondColumn Second targeted column
	  * @param moreColumns More targeted columns
	  * @param connection Implicit DB connection
	  * @return Targeted columns of the targeted item(s)
	  */
	def apply(firstColumn: Column, secondColumn: Column, moreColumns: Column*)(implicit connection: Connection): Seq[V] =
		apply(Pair(firstColumn, secondColumn) ++ moreColumns)
	
	/**
	  * Clears the column (by setting it to NULL) value of all targeted items
	  * @param column Targeted column
	  * @param connection Implicit DB connection
	  * @return Whether any item was targeted
	  */
	def clear(column: Column)(implicit connection: Connection) = update(column, Value.empty)
}

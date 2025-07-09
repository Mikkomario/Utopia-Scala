package utopia.vault.nosql.targeting.columns

import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column

/**
  * An interface that provides access to a single column's values
  * @tparam V Format in which database values are accessed
 * @tparam A Type of parsed column value
  * @tparam In Type of accepted input when assigning values
  * @author Mikko Hilpinen
  * @since 20.05.2025, v1.21
  */
trait ColumnValueAccess[V, +A, -In]
{
	// ABSTRACT ------------------
	
	/**
	  * @return Used database access point
	  */
	protected def access: AccessColumns[V]
	/**
	  * @return Targeted column
	  */
	def column: Column
	
	/**
	  * Parses a column value into a presentable data type
	  * @param value Value to parse
	  * @return Parsed value
	  */
	protected def parse(value: V): A
	/**
	  * Converts an input value into a raw value to store
	  * @param value An input value
	  * @return Value to assign to the column
	  */
	protected def valueOf(value: In): Value
	
	
	// COMPUTED ------------------
	
	/**
	  * @param connection Implicit DB connection
	  * @return The accessible values of this column
	  */
	def pull(implicit connection: Connection) = parse(access(column))
	
	
	// OTHER    -----------------
	
	/**
	  * Assigns a new value to this column
	  * @param newValue New value to assign to this column
	  * @param connection Implicit DB connection
	  * @return Whether any row was targeted
	  */
	def set(newValue: In)(implicit connection: Connection) = access(column) = valueOf(newValue)
	/**
	 * Clears the value of this column, setting it to NULL instead.
	 *
	 * Note: Only works for columns which allow a NULL value.
	 *       The results may vary for other kinds of columns.
	 *       For example, a database exception may be thrown.
	 *
	 * @param connection Implicit DB connection
	 * @return Whether any row was targeted
	 */
	def clear()(implicit connection: Connection) = access(column) = Value.empty
}

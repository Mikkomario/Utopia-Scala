package utopia.vault.nosql.targeting.columns

import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.equality.EqualsFunction
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column

object ColumnValueAccess
{
	implicit class NumericColumnValueAccess[C](val a: ColumnValueAccess[_, _, C, _])(implicit n: Numeric[C])
	{
		/**
		 * @param connection Implicit DB connection
		 * @return The sum of the accessible values
		 */
		def sum(implicit connection: Connection) = a.stream { _.sum }
	}
}

/**
 * An interface that provides access to a single column's values
 * @tparam V Format in which database values are accessed
 * @tparam A Type of parsed column value
 * @tparam C Type of concrete (iterable) column values. May be same as 'A'.
 *
 *           E.g. If 'A' is Option[Int], 'C' would be Int. If 'A' is String, 'C' would also be String.
 *
 * @tparam In Type of accepted input when assigning values
 * @author Mikko Hilpinen
 * @since 20.05.2025, v1.21
 */
trait ColumnValueAccess[V, +A, +C, -In]
{
	// ABSTRACT ------------------
	
	/**
	  * @return Used database access point
	  */
	protected def access: AccessColumns[V, _]
	/**
	  * @return Targeted column
	  */
	def column: Column
	
	/**
	  * Parses column value or values into a presentable data type
	  * @param value Value to parse
	  * @return Parsed value
	  */
	protected def parse(value: V): A
	/**
	  * Converts an input value into a raw value to store
	  * @param value An input value
	  * @return Value to assign to the column
	  */
	def valueOf(value: In): Value
	
	/**
	 * Streams accessible values of this column
	 * @param f A function for processing streamed values
	 * @param connection Implicit DB connection
	 * @tparam B Function result type
	 * @return Function 'f' results
	 */
	def stream[B](f: Iterator[C] => B)(implicit connection: Connection): B
	
	
	// COMPUTED ------------------
	
	/**
	  * @param connection Implicit DB connection
	  * @return The accessible values of this column
	  */
	def pull(implicit connection: Connection) = parse(access(column))
	
	
	// OTHER    -----------------
	
	/**
	 * Checks whether the accessible row(s) contain the specified item / value
	 * @param item Searched column value
	 * @param eq Implicit equals function used (default = use ==)
	 * @param connection Implicit DB connection
	 * @tparam B Type of the searched item
	 * @return Whether accessible data contains the specified item / value
	 */
	def contains[B >: C](item: B)(implicit eq: EqualsFunction[B] = EqualsFunction.default, connection: Connection) =
		containsEqual(item, eq)
	/**
	 * Checks whether the accessible row(s) contain the specified item / value
	 * @param item Searched column value
	 * @param equals Equals function to use
	 * @param connection Implicit DB connection
	 * @tparam B Type of the searched item
	 * @return Whether accessible data contains the specified item / value
	 */
	def containsEqual[B >: C](item: B, equals: EqualsFunction[B])(implicit connection: Connection) =
		stream { _.exists { equals(item, _) } }
	
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

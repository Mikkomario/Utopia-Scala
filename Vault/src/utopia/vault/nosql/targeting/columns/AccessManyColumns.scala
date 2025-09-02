package utopia.vault.nosql.targeting.columns

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column

/**
  * Common trait for access points which target multiple rows at once, providing column value access
  * @author Mikko Hilpinen
  * @since 02.07.2025, v1.22
  */
trait AccessManyColumns extends AccessColumns[Seq[Value], Seq[Seq[Value]]]
{
	// ABSTRACT -------------------------------
	
	/**
	  * Accesses the smallest or largest accessible column value
	  * @param column Targeted column
	  * @param extreme Targeted extreme
	  * @param connection Implicit DB connection
	  * @return The most 'extreme' accessible 'column' value
	  */
	def apply(column: Column, extreme: Extreme)(implicit connection: Connection): Value
	
	/**
	 * @param column Column whose (non-NULL) values are counted
	 * @param distinct Whether to only count distinct column values (default = false = count number of non-NULL entries)
	 * @param connection Implicit DB connection
	 * @return The number of accessible values of that column
	 */
	def count(column: Column, distinct: Boolean = false)(implicit connection: Connection): Int
	
	/**
	  * Accesses values of a single column in a streamed fashion
	  * @param column Targeted column
	  * @param distinct Whether to only target distinct column values (default = false)
	  * @param f A function that processes the streamed column values
	  * @param connection Implicit DB connection
	  * @tparam A Type of function results
	  * @return Function results
	  */
	def streamColumn[A](column: Column, distinct: Boolean = false)(f: Iterator[Value] => A)
	                   (implicit connection: Connection): A
	/**
	  * Accesses column values in a streamed fashion
	  * @param columns Targeted columns
	  * @param f A function that processes the streamed column values.
	  *          Receives a sequence of values collected from a single database row.
	  *          The order and length of this sequence matches that of 'columns'.
	  * @param connection Implicit DB connection
	  * @tparam A Type of function results
	  * @return Function results
	  */
	def streamColumns[A](columns: Seq[Column])(f: Iterator[Seq[Value]] => A)(implicit connection: Connection): A
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param column Targeted column
	  * @param connection Implicit DB connection
	  * @return The smallest accessible value of that column
	  */
	def min(column: Column)(implicit connection: Connection) = apply(column, Min)
	/**
	  * @param column Targeted column
	  * @param connection Implicit DB connection
	  * @return The largest accessible value of that column
	  */
	def max(column: Column)(implicit connection: Connection) = apply(column, Max)
	
	/**
	  * Accesses column values in a streamed fashion
	  * @param f A function that processes the streamed column values.
	  *          Receives a sequence of values collected from a single database row.
	  *          The order and length of this sequence matches that of 'columns'.
	  * @param connection Implicit DB connection
	  * @tparam A Type of function results
	  * @return Function results
	  */
	def streamColumns[A](first: Column, second: Column, more: Column*)(f: Iterator[Seq[Value]] => A)
	                    (implicit connection: Connection): A =
		streamColumns(Pair(first, second) ++ more)(f)
	
	/**
	 * Creates a map from key and value columns
	 * @param key Targeted key column
	 * @param value Targeted value column
	 * @param makeKey A function that converts a value to a key
	 * @param makeValue A function that converts a value to a map value
	 * @param connection Implicit DB connection
	 * @tparam K Type of parsed keys
	 * @tparam V Type of parsed values
	 * @return A map that contains the parsed keys & values
	 */
	def toMap[K, V](key: Column, value: Column)(makeKey: Value => K)(makeValue: Value => V)
	               (implicit connection: Connection) =
		streamColumns(key, value) { iter =>
			iter.map { values =>
				val iter = values.iterator
				makeKey(iter.nextOption().getOrElse(Value.empty)) -> makeValue(iter.nextOption().getOrElse(Value.empty))
			}
			.toMap
		}
	
	/**
	 * Creates a map from key and value columns.
	 * Assumes that there are 0-n values for each key.
	 * @param key Targeted key column
	 * @param value Targeted value column
	 * @param makeKey A function that converts a value to a key
	 * @param makeValues A function that converts read values to a map value
	 * @param connection Implicit DB connection
	 * @tparam K Type of parsed keys
	 * @tparam V Type of parsed values
	 * @return A map that contains the parsed keys & values
	 */
	def toMultiMap[K, V](key: Column, value: Column)(makeKey: Value => K)(makeValues: Seq[Value] => V)
	                    (implicit connection: Connection) =
		apply(key, value)
			.map { values =>
				val iter = values.iterator
				makeKey(iter.nextOption().getOrElse(Value.empty)) -> iter.nextOption().getOrElse(Value.empty)
			}
			.groupMap { _._1 } { _._2 }
			.view.mapValues(makeValues).toMap
}

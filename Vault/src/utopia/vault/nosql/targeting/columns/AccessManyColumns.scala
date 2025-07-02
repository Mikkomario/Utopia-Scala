package utopia.vault.nosql.targeting.columns

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column

/**
  * Common trait for access points which target multiple rows at once, providing column value access
  * @author Mikko Hilpinen
  * @since 02.07.2025, v1.22
  */
trait AccessManyColumns extends AccessColumns[Seq[Value]]
{
	// ABSTRACT -------------------------------
	
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
}

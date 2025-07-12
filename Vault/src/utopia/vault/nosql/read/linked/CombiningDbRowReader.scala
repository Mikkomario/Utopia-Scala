package utopia.vault.nosql.read.linked

import utopia.flow.collection.immutable.Empty
import utopia.vault.model.immutable.{Row, Table}
import utopia.vault.nosql.read.DbRowReader

import scala.util.Try

object CombiningDbRowReader
{
	// OTHER    ---------------------------
	
	/**
	  * @param left Reader used for parsing left side items
	  * @param right Reader used for parsing joined items
	  * @param bridges Joins to perform between the left and the right target (default = empty)
	  * @param merge A function for merging both sides together
	  * @tparam L Type of the parsed left-side items
	  * @tparam R Type of the parsed right-side items
	  * @tparam A Type of the merge results
	  * @return A DB reader that combines the results of the specified two readers
	  */
	def apply[L, R, A](left: DbRowReader[L], right: DbRowReader[R], bridges: Seq[Table] = Empty)
	                  (merge: (L, R) => A): CombiningDbRowReader[L, R, A] =
		new _CombiningDbRowReader[L, R, A](left, right, bridges, merge)
	
	
	// NESTED   ---------------------------
	
	private class _CombiningDbRowReader[L, R, A](left: DbRowReader[L], right: DbRowReader[R],
	                                             bridges: Seq[Table], f: (L, R) => A)
		extends CombiningDbRowReader[L, R, A](left, right, bridges)
	{
		override protected def combine(left: L, right: R): A = f(left, right)
	}
}

/**
  * Common trait for DbRowReaders which are implemented by combining two other readers
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
abstract class CombiningDbRowReader[L, R, +A](left: DbRowReader[L], right: DbRowReader[R],
                                              bridges: Seq[Table] = Empty)
	extends JoiningDbReader[L, R, A](left, right, bridges) with DbRowReader[A]
{
	// ABSTRACT -------------------------
	
	/**
	  * Combines the two read items
	  * @param left Parsed left side item
	  * @param right Parsed right side item
	  * @return Combined result
	  */
	protected def combine(left: L, right: R): A
	
	
	// IMPLEMENTED  ---------------------
	
	override def shouldParse(row: Row): Boolean = left.shouldParse(row) && right.shouldParse(row)
	override def apply(row: Row): Try[A] = left(row).flatMap { left => right(row).map { combine(left, _) } }
}

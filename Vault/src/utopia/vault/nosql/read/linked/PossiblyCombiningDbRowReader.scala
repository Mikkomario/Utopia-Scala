package utopia.vault.nosql.read.linked

import utopia.flow.collection.immutable.Empty
import utopia.vault.model.immutable.{Row, Table}
import utopia.vault.model.template.ConditionallyJoinable
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.sql.{Condition, JoinType}

import scala.util.Try

object PossiblyCombiningDbRowReader
{
	// OTHER    ---------------------------
	
	/**
	  * @param left Reader used for parsing left side items
	  * @param right Reader used for parsing joined items
	  * @param bridges Joins to perform between the left and the right target (default = empty)
	  * @param joinConditions Conditions that must be met in order for joining to occur (default = empty)
	 * @param merge A function for merging both sides together
	  * @tparam L Type of the parsed left-side items
	  * @tparam R Type of the parsed right-side items
	  * @tparam A Type of the merge results
	  * @return A DB reader that combines the results of the specified two readers
	  */
	def apply[L, R, A](left: DbRowReader[L], right: DbRowReader[R],
	                   bridges: Seq[Table] = Empty, joinConditions: Seq[Condition] = Empty)
	                  (merge: (L, Option[R]) => A): PossiblyCombiningDbRowReader[L, R, A] =
		new _PossiblyCombiningDbRowReader[L, R, A](left, right, bridges, joinConditions, merge)
	
	
	// NESTED   ---------------------------
	
	private class _PossiblyCombiningDbRowReader[L, R, A](left: DbRowReader[L],
	                                                     right: DbRowReader[R],
	                                                     bridges: Seq[Table], joinConditions: Seq[Condition],
	                                                     f: (L, Option[R]) => A)
		extends PossiblyCombiningDbRowReader[L, R, A](left, right, bridges, joinConditions)
	{
		override protected def combine(left: L, right: Option[R]): A = f(left, right)
	}
}

/**
  * Common trait for DbRowReaders which are implemented by combining two other readers using left join
  * and optional combining.
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
abstract class PossiblyCombiningDbRowReader[L, R, +A](left: DbRowReader[L],
                                                      right: DbRowReader[R],
                                                      bridges: Seq[Table] = Empty,
                                                      joinConditions: Seq[Condition] = Empty)
	extends JoiningDbReader[L, R, A](left, right, bridges, JoinType.Left, joinConditions) with DbRowReader[A]
		with ConditionallyJoinable[DbRowReader[A]]
{
	// ABSTRACT -------------------------
	
	/**
	  * Combines the two read items
	  * @param left Parsed left side item
	  * @param right Parsed right side item, if one was present
	  * @return Combined result
	  */
	protected def combine(left: L, right: Option[R]): A
	
	
	// IMPLEMENTED  ---------------------
	
	override def shouldParse(row: Row): Boolean = left.shouldParse(row)
	override def apply(row: Row): Try[A] = left(row).map { combine(_, right.tryParse(row)) }
	
	override def onlyJoinIf(condition: Condition): DbRowReader[A] = {
		if (condition.isAlwaysTrue)
			this
		else
			PossiblyCombiningDbRowReader(left, right, bridges, joinConditions :+ condition)(combine)
	}
	override def onlyJoinIf(conditions: Seq[Condition]): DbRowReader[A] = {
		if (conditions.isEmpty)
			this
		else
			PossiblyCombiningDbRowReader(left, right, bridges, joinConditions ++ conditions)(combine)
	}
}

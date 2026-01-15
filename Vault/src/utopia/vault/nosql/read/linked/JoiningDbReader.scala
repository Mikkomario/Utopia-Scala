package utopia.vault.nosql.read.linked

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.Table
import utopia.vault.model.mutable.ResultStream
import utopia.vault.model.template.ConditionallyJoinable
import utopia.vault.nosql.read.{DbReader, DbRowReader}
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Condition, JoinType, SqlTarget}

object JoiningDbReader
{
	// OTHER    ---------------------------
	
	/**
	 * Creates a new DB reader by joining two other readers
	 * @param left The left side reader to use
	 * @param right The right side reader to use
	 * @param bridges Joins to perform between the left and the right target (default = empty)
	 * @param joinType Type of join applied (default = inner)
	 * @param joinConditions Conditions that must be met in order for joining to occur (default = empty)
	 * @param parse A function which parses the result stream
	 * @tparam L Type of the parsed items on the left / primary side
	 * @tparam R Type of the parsed items on the right / joined side
	 * @tparam A Type of the combined items
	 * @return A new DB reader
	 */
	def apply[L, R, A](left: DbRowReader[L], right: DbRowReader[R], bridges: Seq[Table] = Empty,
	                   joinType: JoinType = Inner, joinConditions: Seq[Condition] = Empty)
	                  (parse: ResultStream => Seq[A]): JoiningDbReader[L, R, A] =
		new _JoiningDbReader[L, R, A](left, right, bridges, joinType, joinConditions, parse)
	
	
	// NESTED   ---------------------------
	
	private class _JoiningDbReader[+L, +R, +A](left: DbRowReader[L], right: DbRowReader[R], bridges: Seq[Table],
	                                           joinType: JoinType, joinConditions: Seq[Condition],
	                                           parse: ResultStream => Seq[A])
		extends JoiningDbReader[L, R, A](left, right, bridges, joinType, joinConditions)
	{
		override def apply(stream: ResultStream): Seq[A] = parse(stream)
	}
}

/**
  * Common abstract implementation for DbReaders which are implemented by somehow combining two other DB row readers
  * @tparam L Type of the parsed items on the left / primary side
  * @tparam R Type of the parsed items on the right / joined side
  * @tparam A Type of the combined items
  * @param left The left side reader to use
  * @param right The right side reader to use
  * @param bridges Joins to perform between the left and the right target (default = empty)
  * @param joinType Type of join applied (default = inner)
  * @param joinConditions Conditions that must be met in order for joining to occur (default = empty)
 * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
abstract class JoiningDbReader[+L, +R, +A](protected val left: DbRowReader[L], protected val right: DbRowReader[R],
                                           bridges: Seq[Table] = Empty, joinType: JoinType = Inner,
                                           joinConditions: Seq[Condition] = Empty)
	extends DbReader[Seq[A]] with ConditionallyJoinable[DbReader[Seq[A]]]
{
	// ATTRIBUTES   ----------------------
	
	private val rightTables = right.tables
	override val tables: Seq[Table] = OptimizedIndexedSeq.concat(left.tables, bridges, rightTables)
	override val target: SqlTarget = {
		val joinTargets = {
			// Case: Using LEFT JOIN => Left-joins all the right-side tables
			if (joinType == JoinType.Left)
				scala.collection.View.concat(bridges, rightTables).map { _ -> joinType }.toOptimizedSeq
			// Case: Using INNER or RIGHT JOIN => Applies the right reader's join types (which may include left joins)
			else
				OptimizedIndexedSeq.concat(
					bridges.view.map { _ -> joinType },
					rightTables.view.zip((joinType +: right.target.joinTypes).padTo(rightTables.size, joinType))
				)
		}
		val appliedConditions = joinConditions.filterNot { _.isAlwaysTrue }
		
		// Applies the joins as conditional, if applicable
		val joined = {
			// No join conditions applied => Just joins the tables
			if (appliedConditions.isEmpty)
				joinTargets
			// Case: Join conditions applicable
			else
				joinTargets.oneOrMany match {
					// Case: Only one table joined => All conditions are applied to that table
					case Left((table, joinType)) =>
						Single(table.onlyJoinIf(Condition.and(appliedConditions)) -> joinType)
					// Case: Multiple tables joined => Join conditions are applied to the tables which they concern
					case Right(joinTargets) =>
						val conditionByTable = appliedConditions.iterator
							.map { condition =>
								val table = condition.toWhereClause.targetTables.find(tables.contains)
									// If a condition doesn't concern any of the joined tables,
									// links it with the primary joined table
									.getOrElse(right.table)
								table -> condition
							}
							.groupMapReduce { _._1 } { _._2 } { _ && _ }
						
						// Applies the join conditions
						joinTargets.map { case (table, joinType) =>
							val conditionalTable = conditionByTable.get(table) match {
								case Some(condition) => table.onlyJoinIf(condition)
								case None => table
							}
							conditionalTable -> joinType
						}
				}
		}
		joined.iterator.groupConsecutiveBy { _._2 }
			.foldLeft(left.target) { case (left, (joinType, right)) => left.join(right.view.map { _._1 }, joinType) }
	}
	override val selectTarget: SelectTarget = left.selectTarget + right.selectTarget
	
	
	// IMPLEMENTED  ----------------------
	
	override def table: Table = left.table
	
	override def onlyJoinIf(condition: Condition): DbReader[Seq[A]] = {
		if (condition.isAlwaysTrue)
			this
		else
			JoiningDbReader(left, right, bridges, joinType, this.joinConditions :+ condition)(apply)
	}
	override def onlyJoinIf(conditions: Seq[Condition]): DbReader[Seq[A]] = {
		if (conditions.isEmpty)
			this
		else
			JoiningDbReader(left, right, bridges, joinType, this.joinConditions ++ conditions)(apply)
	}
}

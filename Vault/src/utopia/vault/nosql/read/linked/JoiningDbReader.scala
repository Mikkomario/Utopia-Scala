package utopia.vault.nosql.read.linked

import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq}
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.Table
import utopia.vault.nosql.read.{DbReader, DbRowReader}
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{JoinType, SqlTarget}

/**
  * Common abstract implementation for DbReaders which are implemented by somehow combining two other DB row readers
  * @tparam L Type of the parsed items on the left / primary side
  * @tparam R Type of the parsed items on the right / joined side
  * @tparam A Type of the combined items
  * @param left The left side reader to use
  * @param right The right side reader to use
  * @param bridges Joins to perform between the left and the right target (default = empty)
  * @param joinType Type of join applied (default = inner)
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
abstract class JoiningDbReader[+L, +R, +A](protected val left: DbRowReader[L], protected val right: DbRowReader[R],
                                           bridges: Seq[Table] = Empty, joinType: JoinType = Inner)
	extends DbReader[Seq[A]]
{
	// ATTRIBUTES   ----------------------
	
	override lazy val tables: Seq[Table] = OptimizedIndexedSeq.concat(left.tables, bridges, right.tables)
	override lazy val target: SqlTarget = left.target.join(bridges ++ right.tables, joinType)
	override lazy val selectTarget: SelectTarget = left.selectTarget + right.selectTarget
	
	
	// IMPLEMENTED  ----------------------
	
	override def table: Table = left.table
}

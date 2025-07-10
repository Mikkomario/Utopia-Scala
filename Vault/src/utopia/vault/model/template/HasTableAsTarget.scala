package utopia.vault.model.template

import utopia.flow.collection.immutable.Empty
import utopia.vault.model.immutable.Table
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{JoinType, SqlTarget}

/**
  * Common trait for classes which use a single Table as a target for SQL queries
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
trait HasTableAsTarget extends HasTable with HasTarget with HasTablesAsTarget
{
	override def target: SqlTarget = table
	
	// Does not apply any joins
	override def joinedTables: Seq[Table] = Empty
	override def joinType: JoinType = Inner
}

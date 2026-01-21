package utopia.vault.nosql.targeting.grouped

import utopia.vault.model.error.ColumnNotFoundException
import utopia.vault.model.immutable.Table
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.template.Filterable
import utopia.vault.sql.{Condition, JoinType, SqlTarget}

/**
  * Common trait for concrete AccessManyLike implementations (i.e. non-wrappers)
  * @author Mikko Hilpinen
  * @since 24.06.2025, v1.21.1
  */
trait ConcreteAccessGroupedLike[+A, +Repr <: Filterable[Repr]] extends AccessGroupedLike[A, Repr]
{
	// ABSTRACT --------------------------
	
	/**
	  * @param target New target to assign (default = current)
	  * @param accessCondition New access condition to apply (default = current)
	  * @return A copy of this access point with the specified target & access condition
	  */
	protected def copyAccess(target: SqlTarget = target, accessCondition: Option[Condition] = accessCondition): Repr
	
	
	// IMPLEMENTED  ----------------------
	
	override def join(joins: Seq[Joinable], joinType: JoinType) = {
		if (joins.isEmpty)
			self
		else {
			val newTarget = target.join(joins, joinType)
			if (newTarget == target)
				self
			else
				copyAccess(target = newTarget)
		}
	}
	override def notLinkedTo(table: Table, where: Option[Condition]) = {
		table.primaryColumn match {
			case Some(index) =>
				val joinToApply = where match {
					case Some(condition) => table.onlyJoinIf(condition)
					case None => table
				}
				leftJoin(joinToApply).filter(index.isNull)
				
			case None => throw new ColumnNotFoundException(s"$table doesn't have a primary key")
		}
	}
	override def apply(condition: Condition) = {
		// Extends the current target to include the specified condition's tables
		val extendedTarget = (condition.segment.targetTables -- target.tables).foldLeft(target) { _.join(_) }
		copyAccess(target = extendedTarget, accessCondition = Some(condition))
	}
}

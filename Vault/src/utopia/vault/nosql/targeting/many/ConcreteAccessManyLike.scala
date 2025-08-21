package utopia.vault.nosql.targeting.many

import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.vault.model.error.ColumnNotFoundException
import utopia.vault.model.immutable.Table
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.sql.{Condition, JoinType, OrderBy, SqlTarget}

/**
  * Common trait for concrete AccessManyLike implementations (i.e. non-wrappers)
  * @author Mikko Hilpinen
  * @since 24.06.2025, v1.21.1
  */
trait ConcreteAccessManyLike[+A, +Repr <: TargetingManyLike[A, Repr, _]] extends AccessManyLike[A, Repr]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Copy of this access point that's limited to one row / item, if applicable.
	  *         Access points which don't support limits should yield themselves.
	  */
	protected def limitedToOne: Repr
	
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
			val newTarget = target.join(joins)
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
					case Some(condition) => table.where(condition)
					case None => table
				}
				join(joinToApply).filter(index.isNull)
				
			case None => throw new ColumnNotFoundException(s"$table doesn't have a primary key")
		}
	}
	override def apply(condition: Condition) = {
		// Extends the current target to include the specified condition's tables
		val extendedTarget = (condition.segment.targetTables -- target.tables).foldLeft(target) { _.join(_) }
		copyAccess(target = extendedTarget, accessCondition = Some(condition))
	}
	
	override def apply(end: End, ordering: Option[OrderBy], filter: Option[Condition]) = {
		// Applies the correct ordering
		val access = {
			if (ordering.isEmpty && (end == First || this.ordering.isEmpty))
				limitedToOne
			else
				ordering match {
					case Some(ordering) =>
						limitedToOne.withOrdering(end match {
							case First => ordering
							case Last => -ordering
						})
					case None =>
						end match {
							case First => limitedToOne
							case Last =>
								ordering match {
									case Some(ordering) => limitedToOne.withOrdering(-ordering)
									case None => limitedToOne
								}
						}
				}
		}
		// Creates a view to the first element in this access point
		TargetingOne.headOf[Repr, A](access.filter(filter))
	}
}

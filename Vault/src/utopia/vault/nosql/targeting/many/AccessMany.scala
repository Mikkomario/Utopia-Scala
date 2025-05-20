package utopia.vault.nosql.targeting.many

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Identity
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.util.Mutate
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.{Result, Table}
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.nosql.template.Deprecatable
import utopia.vault.sql.{Condition, JoinType, OrderBy, SqlSegment, SqlTarget}

object AccessMany
{
	// OTHER    ------------------------------
	
	def apply[A](factory: FromResultFactory[A]): AccessMany[A] = _AccessMany[A](
		factory.target, factory.table, factory.selectTarget, factory.apply, ordering = factory.defaultOrdering)
	
	def apply[A](target: SqlTarget, table: Table, selectTarget: SelectTarget, condition: Option[Condition] = None,
	             ordering: Option[OrderBy] = None, prepare: Mutate[SqlSegment] = Identity)
	            (f: Result => Seq[A]): AccessMany[A] =
		_AccessMany[A](target, table, selectTarget, f, condition, ordering, prepare)
	
	def table[A](table: Table, condition: Option[Condition] = None, ordering: Option[OrderBy] = None,
	             prepare: Mutate[SqlSegment] = Identity)
	            (f: Result => Seq[A]) =
		apply[A](table, table, SelectTarget.table(table), condition, ordering, prepare)(f)
	
	def tables[A](first: Table, second: Table, more: Table*)(f: Result => Seq[A]): AccessMany[A] =
		apply[A](more.foldLeft(first join second) { _ join _ }, first,
			SelectTarget.tables(Pair(first, second) ++ more))(f)
	
	def active[A](factory: FromResultFactory[A] with Deprecatable): AccessMany[A] =
		apply(factory).filter(factory.nonDeprecatedCondition)
	
	
	// NESTED   ------------------------------
	
	private case class _AccessMany[+A](target: SqlTarget, table: Table, selectTarget: SelectTarget, f: Result => Seq[A],
	                                   accessCondition: Option[Condition] = None, ordering: Option[OrderBy] = None,
	                                   prepare: Mutate[SqlSegment] = Identity)
		extends AccessMany[A]
	{
		// IMPLEMENTED  ----------------------
		
		override protected def self: AccessMany[A] = this
		
		override protected def finalizeStatement(statement: SqlSegment) = prepare(statement)
		
		override protected def parse(result: Result) = f(result)
		
		override def join(joins: Seq[Joinable], joinType: JoinType) =
			if (joins.isEmpty) this else copy(target = joins.foldLeft(target) { _.join(_, joinType) })
		
		override def apply(condition: Condition): AccessMany[A] = {
			// Extends the current target to include the specified condition's tables
			val extendedTarget = (condition.segment.targetTables -- target.tables).foldLeft(target) { _.join(_) }
			copy(target = extendedTarget, accessCondition = Some(condition))
		}
		
		override def apply(end: End, ordering: Option[OrderBy]) = {
			// Applies the correct ordering
			val access = {
				if (ordering.isEmpty && (end == First || this.ordering.isEmpty))
					this
				else
					ordering match {
						case Some(ordering) =>
							withOrdering(end match {
								case First => ordering
								case Last => -ordering
							})
						case None =>
							end match {
								case First => this
								case Last =>
									ordering match {
										case Some(ordering) => withOrdering(-ordering)
										case None => this
									}
							}
					}
			}
			// Creates a view to the first element in this access point
			TargetingOne.headOf[AccessMany[A], A](access)
		}
		
		override def withOrdering(ordering: OrderBy): AccessMany[A] = copy(ordering = Some(ordering))
	}
}

/**
  * Common trait for extendable & filterable access points that yield multiple items at once
  * @author Mikko Hilpinen
  * @since 16.05.2025, v1.21
  */
trait AccessMany[+A] extends TargetingMany[A] with AccessManyLike[A, AccessMany[A]]

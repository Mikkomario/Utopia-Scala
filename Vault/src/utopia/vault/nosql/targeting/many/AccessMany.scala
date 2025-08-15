package utopia.vault.nosql.targeting.many

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.Identity
import utopia.flow.util.Mutate
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.Table
import utopia.vault.model.mutable.ResultStream
import utopia.vault.model.template.Deprecates
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.nosql.read.DbReader
import utopia.vault.nosql.read.parse.ParseResultStream
import utopia.vault.sql.{Condition, OrderBy, SqlSegment, SqlTarget}

object AccessMany
{
	// OTHER    ------------------------------
	
	def apply[A](factory: DbReader[Seq[A]]): AccessMany[A] =
		_AccessMany[A](factory.target, factory.table, factory.selectTarget, factory)
	def apply[A](factory: FromResultFactory[A], useDefaultOrdering: Boolean): AccessMany[A] =
		_AccessMany[A](factory.target, factory.table, factory.selectTarget, factory,
			ordering = if (useDefaultOrdering) factory.defaultOrdering else None)
	
	def apply[A](target: SqlTarget, table: Table, selectTarget: SelectTarget, condition: Option[Condition] = None,
	             ordering: Option[OrderBy] = None, prepare: Mutate[SqlSegment] = Identity)
	            (parse: ParseResultStream[Seq[A]]): AccessMany[A] =
		_AccessMany[A](target, table, selectTarget, parse, condition, ordering, prepare)
	
	def table[A](table: Table, condition: Option[Condition] = None, ordering: Option[OrderBy] = None,
	             prepare: Mutate[SqlSegment] = Identity)
	            (parse: ParseResultStream[Seq[A]]) =
		apply[A](table, table, SelectTarget.table(table), condition, ordering, prepare)(parse)
	
	def tables[A](first: Table, second: Table, more: Table*)(parse: ParseResultStream[Seq[A]]): AccessMany[A] =
		apply[A](more.foldLeft(first join second) { _ join _ }, first,
			SelectTarget.tables(Pair(first, second) ++ more))(parse)
	
	def active[A](factory: DbReader[Seq[A]] with Deprecates): AccessMany[A] =
		apply(factory).filter(factory.activeCondition)
	
	
	// NESTED   ------------------------------
	
	private case class _AccessMany[+A](target: SqlTarget, table: Table, selectTarget: SelectTarget,
	                                   f: ParseResultStream[Seq[A]],
	                                   accessCondition: Option[Condition] = None, ordering: Option[OrderBy] = None,
	                                   prepare: Mutate[SqlSegment] = Identity)
		extends ConcreteAccessManyLike[A, AccessMany[A]] with AccessMany[A]
	{
		// IMPLEMENTED  ----------------------
		
		override protected def self: AccessMany[A] = this
		override protected def limitedToOne = this
		
		override protected def finalizeStatement(statement: SqlSegment) = prepare(statement)
		
		override protected def parse(result: ResultStream) = f(result)
		
		override def withOrdering(ordering: OrderBy): AccessMany[A] = copy(ordering = Some(ordering))
		override protected def copyAccess(target: SqlTarget, accessCondition: Option[Condition]) =
			copy(target = target, accessCondition = accessCondition)
	}
}

/**
  * Common trait for extendable & filterable access points that yield multiple items at once
  * @author Mikko Hilpinen
  * @since 16.05.2025, v1.21
  */
trait AccessMany[+A] extends TargetingMany[A] with AccessManyLike[A, AccessMany[A]]

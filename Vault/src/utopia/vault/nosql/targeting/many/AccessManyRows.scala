package utopia.vault.nosql.targeting.many

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.view.immutable.caching.Lazy
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.{Column, Result, Row, Table}
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.nosql.template.Deprecatable
import utopia.vault.sql.{Condition, JoinType, OrderBy, SqlTarget}

import scala.collection.View

object AccessManyRows
{
	// OTHER    --------------------------
	
	def apply[A](factory: FromRowFactory[A]): AccessManyRows[A] =
		apply(factory.target, factory.table, factory.selectTarget, ordering = factory.defaultOrdering)(
			factory.parseIfPresent)
	
	def apply[A](target: SqlTarget, table: Table, selectTarget: SelectTarget, condition: Option[Condition] = None,
	             ordering: Option[OrderBy] = None, limit: Option[Int] = None, offset: Int = 0,
	             uniqueIndices: Boolean = false)
	            (f: Row => Option[A]): AccessManyRows[A] =
		_AccessManyRows[A](target, table, selectTarget, f, condition, ordering, limit, offset, uniqueIndices)
		
	def table[A](table: Table, condition: Option[Condition] = None, ordering: Option[OrderBy] = None,
	             limit: Option[Int] = None, offset: Int = 0, uniqueIndices: Boolean = false)
	            (f: Row => Option[A]) =
		apply(table, table, SelectTarget.table(table), condition, ordering, limit, offset, uniqueIndices)(f)
		
	def tables[A](first: Table, second: Table, more: Table*)(f: Row => Option[A]) =
		apply(more.foldLeft(first join second) { _ join _ }, first, SelectTarget.tables(Pair(first, second) ++ more))(f)
	
	def valid[A](factory: FromRowFactory[A] with Deprecatable) =
		apply(factory).filter(factory.nonDeprecatedCondition)
	
	
	// NESTED   --------------------------
	
	private case class _AccessManyRows[+A](target: SqlTarget, table: Table, selectTarget: SelectTarget,
	                                       f: Row => Option[A], accessCondition: Option[Condition] = None,
	                                       ordering: Option[OrderBy] = None, limit: Option[Int] = None, offset: Int = 0,
	                                       limitsToUniqueIndices: Boolean = false)
		extends AccessManyRows[A]
	{
		// ATTRIBUTES   -----------------
		
		override protected lazy val keys = super.keys
		
		
		// IMPLEMENTED  -----------------
		
		override protected def self = this
		
		override def apply(condition: Condition) = copy(accessCondition = Some(condition))
		override def withOrdering(ordering: OrderBy) = copy(ordering = Some(ordering))
		override def withLimit(limit: Int) = copy(limit = Some(limit))
		override def withOffset(offset: Int, limit: Option[Int]) =
			copy(offset = offset, limit = limit)
		override def withLimitToUniqueIndices(limit: Boolean) = copy(limitsToUniqueIndices = limit)
		
		override def join(joins: Seq[Joinable], joinType: JoinType) =
			if (joins.isEmpty) this else copy(target = joins.foldLeft(target) { _.join(_, joinType) })
		
		override protected def parse(row: Row) = f(row)
		
		override def extendTo[B](tables: Seq[Table], exclusiveColumns: Seq[Column], bridgingJoins: Seq[Joinable],
		                         joinType: JoinType)
		                        (f: Seq[(A, Row)] => Seq[B]) =
			_extendTo(tables, exclusiveColumns, bridgingJoins, joinType) { (result, lazyIndices) =>
				if (limitsToUniqueIndices)
					f(result.rows.map { row => row -> lazyIndices.value.iterator.map(row.apply).caching }
						.iterator.distinctBy { _._2 }
						.flatMap { case (row, _) => parse(row).map { _ -> row } }
						.toOptimizedSeq)
				else
					f(result.rows.flatMap { row => parse(row).map { _ -> row } })
			}
		
		override def extendToMany[B](tables: Seq[Table], exclusiveColumns: Seq[Column], bridgingJoins: Seq[Joinable],
		                             joinType: JoinType)
		                            (f: Seq[(A, Seq[Row])] => Seq[B]) =
			_extendTo(tables, exclusiveColumns, bridgingJoins, joinType) { (result, indices) =>
				f(result.rows.groupBy { row => indices.value.map(row.apply) }.valuesIterator
					.flatMap { rows => parse(rows.head).map { _ -> rows } }
					.toOptimizedSeq)
			}
		
		override def apply(end: End, ordering: Option[OrderBy]) = {
			val limited = if (limit.contains(1)) this else withLimit(1)
			val ordered = {
				if (ordering.isEmpty && (end == First || limited.ordering.isEmpty))
					limited
				else
					ordering match {
						case Some(ordering) =>
							limited.withOrdering(end match {
								case First => ordering
								case Last => -ordering
							})
						case None =>
							limited.ordering match {
								case Some(ordering) =>
									end match {
										case First => limited
										case Last => limited.withOrdering(-ordering)
									}
								case None => limited
							}
					}
			}
			TargetingOne.headOf[AccessManyRows[A], A](ordered)
		}
		
		
		// OTHER    ------------------------
		
		private def _extendTo[B](tables: Seq[Table], exclusiveColumns: Seq[Column], bridgingJoins: Seq[Joinable],
		                         joinType: JoinType)
		                        (f: (Result, Lazy[Seq[Column]]) => Seq[B]) =
		{
			val newTarget = tables.foldLeft(
				bridgingJoins.foldLeft(target) { _.join(_, joinType) }) { _.join(_, joinType) }
			val addedSelectTargets = tables.map { table =>
				val exclusiveCols = exclusiveColumns.filter(table.contains)
				if (exclusiveCols.isEmpty)
					SelectTarget.table(table)
				else
					SelectTarget.columns(exclusiveCols)
			}
			val newSelectTarget = addedSelectTargets.foldLeft(selectTarget) { _ + _ }
			val lazyKeys = Lazy {
				View.concat(target.tables, tables).flatMap { _.primaryColumn }.filter(newSelectTarget.contains)
					.toOptimizedSeq
			}
			
			AccessMany(newTarget, table, newSelectTarget, accessCondition, ordering, prepare = finalizeStatement) {
				f(_, lazyKeys) }
		}
	}
}

/**
  * Common trait for extendable & filterable access points that yield multiple row-specific items at once
  * @author Mikko Hilpinen
  * @since 18.05.2025, v1.21
  */
trait AccessManyRows[+A] extends AccessMany[A] with TargetingManyRows[A] with AccessManyRowsLike[A, AccessManyRows[A]]

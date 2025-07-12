package utopia.vault.nosql.targeting.many

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Pair}
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.{Column, Row, Table}
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.template.Deprecatable
import utopia.vault.sql.{Condition, JoinType, OrderBy, SqlTarget}

object AccessManyRows
{
	// OTHER    --------------------------
	
	def apply[A](factory: DbRowReader[A]): AccessManyRows[A] =
		apply(factory.target, factory.table, factory.selectTarget)(factory.tryParse)
	def apply[A](factory: FromRowFactory[A], useDefaultOrdering: Boolean): AccessManyRows[A] =
		apply(factory.target, factory.table, factory.selectTarget,
			ordering = if (useDefaultOrdering) factory.defaultOrdering else None)(factory.tryParse)
	
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
	
	/**
	  * @param factory Factory to wrap
	  * @tparam A Type of parsed items
	  * @return Access to items targeted by that factory. Limited to active (i.e. non-deprecated) items.
	  */
	def active[A](factory: DbRowReader[A] with Deprecatable) =
		apply(factory).filter(factory.nonDeprecatedCondition)
	
	
	// NESTED   --------------------------
	
	private case class _AccessManyRows[+A](target: SqlTarget, table: Table, selectTarget: SelectTarget,
	                                       f: Row => Option[A], accessCondition: Option[Condition] = None,
	                                       ordering: Option[OrderBy] = None, limit: Option[Int] = None, offset: Int = 0,
	                                       limitsToUniqueIndices: Boolean = false)
		extends ConcreteAccessManyLike[A, AccessManyRows[A]] with AccessManyRows[A]
	{
		// ATTRIBUTES   -----------------
		
		override protected lazy val keys = super.keys
		
		
		// IMPLEMENTED  -----------------
		
		override protected def self = this
		override protected def limitedToOne = if (limit.contains(1)) this else withLimit(1)
		
		override def withOrdering(ordering: OrderBy) = copy(ordering = Some(ordering))
		override def withLimit(limit: Int) = copy(limit = Some(limit))
		override def withOffset(offset: Int, limit: Option[Int]) =
			copy(offset = offset, limit = limit)
		override def withLimitToUniqueIndices(limit: Boolean) = copy(limitsToUniqueIndices = limit)
		
		override protected def copyAccess(target: SqlTarget, accessCondition: Option[Condition]) =
			copy(target = target, accessCondition = accessCondition)
		
		override protected def parse(row: Row) = f(row)
		
		override def extendTo[B](tables: Seq[Table], exclusiveColumns: Seq[Column], bridges: Seq[Joinable],
		                         joinType: JoinType)
		                        (f: (A, Row) => Option[B]) =
			_extendTo(tables, exclusiveColumns, bridges, joinType) { (newTarget, newSelect) =>
				copy(target = newTarget, selectTarget = newSelect, f = row => parse(row).flatMap { f(_, row) })
			}
		
		override def extendToMany[B](tables: Seq[Table], exclusiveColumns: Seq[Column], bridges: Seq[Joinable],
		                             joinType: JoinType)
		                            (f: Iterator[(A, Seq[Row])] => IterableOnce[B]) =
			_extendTo(tables, exclusiveColumns, bridges, joinType) { (newTarget, newSelect) =>
				val indices = target.tables.view.flatMap { _.primaryColumn }.filter(selectTarget.contains)
					.toOptimizedSeq
				AccessMany(newTarget, table, newSelect, accessCondition, ordering, prepare = finalizeStatement) {
					result =>
						// NB: Assumes that same index rows are consecutive
						OptimizedIndexedSeq.from(f(result.rowsIterator.groupBy { row => indices.map(row.apply) }
							.flatMap { case (_, rows) => parse(rows.head).map { _ -> rows } }))
				}
			}
		
		
		// OTHER    ------------------------
		
		private def _extendTo[B](tables: Seq[Table], exclusiveColumns: Seq[Column], bridgingJoins: Seq[Joinable],
		                         joinType: JoinType)
		                        (formAccess: (SqlTarget, SelectTarget) => B) =
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
			
			formAccess(newTarget, newSelectTarget)
		}
	}
}

/**
  * Common trait for extendable & filterable access points that yield multiple row-specific items at once
  * @author Mikko Hilpinen
  * @since 18.05.2025, v1.21
  */
trait AccessManyRows[+A] extends AccessMany[A] with TargetingManyRows[A] with AccessManyRowsLike[A, AccessManyRows[A]]

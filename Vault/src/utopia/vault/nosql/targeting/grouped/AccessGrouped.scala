package utopia.vault.nosql.targeting.grouped

import utopia.flow.operator.Identity
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.Table
import utopia.vault.model.mutable.ResultStream
import utopia.vault.nosql.read.DbReader
import utopia.vault.nosql.read.parse.ParseResultStream
import utopia.vault.sql.{Condition, OrderBy, SqlSegment, SqlTarget}

object AccessGrouped
{
	// OTHER    ----------------------------
	
	/**
	 * Creates a new access point into an individual table's data
	 * @param table Targeted table
	 * @param emptyResultView A view into an empty parsed item (one representing 0 rows)
	 * @param condition A condition applied to every query (default = None)
	 * @param ordering Applied ordering (default = None)
	 * @param prepare A finalization function applied to SQL statements before they're executed (default = identity)
	 * @param parse A function for parsing the acquired result stream / streams
	 * @tparam A Type of the parsed results
	 * @return A new access interface
	 */
	def table[A](table: Table, emptyResultView: View[A],
	             condition: Option[Condition] = None, ordering: Option[OrderBy] = None,
	             prepare: Mutate[SqlSegment] = Identity)(parse: ParseResultStream[A]) =
		apply(table, table, SelectTarget.table(table), emptyResultView, condition, ordering, prepare)(parse)
	
	/**
	 * Wraps a database reader
	 * @param reader Reader to wrap
	 * @param emptyResultView A view into an empty parsed item (one representing 0 rows)
	 * @tparam A Type of the parsed results / read items or groups of items
	 * @return A new access point that reads data using the specified reader
	 */
	def apply[A](reader: DbReader[A], emptyResultView: View[A]): AccessGrouped[A] =
		apply(reader.target, reader.table, reader.selectTarget, emptyResultView)(reader)
	
	/**
	 * Creates a new access point
	 * @param target Applied SQL target (table or tables)
	 * @param table The primarily targeted table
	 * @param selectTarget Applied select target (the column data that is actually pulled)
	 * @param emptyResultView A view into an empty parsed item (one representing 0 rows)
	 * @param condition A condition applied to every query (default = None)
	 * @param ordering Applied ordering (default = None)
	 * @param prepare A finalization function applied to SQL statements before they're executed (default = identity)
	 * @param parse A function for parsing the acquired result stream / streams
	 * @tparam A Type of the parsed results
	 * @return A new access interface
	 */
	def apply[A](target: SqlTarget, table: Table, selectTarget: SelectTarget, emptyResultView: View[A],
	             condition: Option[Condition] = None, ordering: Option[OrderBy] = None,
	             prepare: Mutate[SqlSegment] = Identity)
	            (parse: ParseResultStream[A]): AccessGrouped[A] =
		_AccessGrouped[A](target, table, selectTarget, parse, emptyResultView, condition, ordering, prepare)
	
	
	// NESTED   ----------------------------
	
	private case class _AccessGrouped[+A](target: SqlTarget, table: Table, selectTarget: SelectTarget,
	                                      f: ParseResultStream[A], emptyResultView: View[A],
	                                      accessCondition: Option[Condition] = None, ordering: Option[OrderBy] = None,
	                                      prepare: Mutate[SqlSegment] = Identity)
		extends AccessGrouped[A] with ConcreteAccessGroupedLike[A, AccessGrouped[A]]
	{
		override def self: AccessGrouped[A] = this
		
		override protected def emptyResult: A = emptyResultView.value
		
		override protected def finalizeStatement(statement: SqlSegment): SqlSegment = prepare(statement)
		override protected def parse(result: ResultStream): A = f(result)
		
		override def withOrdering(ordering: OrderBy): AccessGrouped[A] = copy(ordering = Some(ordering))
		
		override protected def copyAccess(target: SqlTarget, accessCondition: Option[Condition]): AccessGrouped[A] =
			copy(target = target, accessCondition = accessCondition)
	}
}

/**
 * A version of [[AccessGroupedLike]], which specifies a (default) Repr type.
 * @tparam A Type of the pulled groups of items
 * @author Mikko Hilpinen
 * @since 21.01.2026, v2.1
 */
trait AccessGrouped[+A] extends AccessGroupedLike[A, AccessGrouped[A]]

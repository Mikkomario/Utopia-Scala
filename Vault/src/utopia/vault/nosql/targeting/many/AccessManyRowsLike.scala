package utopia.vault.nosql.targeting.many

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.{Row, TableColumn}
import utopia.vault.model.mutable.ResultStream
import utopia.vault.nosql.targeting.one.TargetingOne
import utopia.vault.sql.{Limit, Offset, SqlSegment}

/**
  * Common trait for interfaces that are used for querying multiple row-specific instances from the DB at once,
  * using a FromRowFactory
  * @author Mikko Hilpinen
  * @since 16.05.2025, v1.21
  */
trait AccessManyRowsLike[+A, +Repr]
	extends AccessManyLike[A, Repr] with TargetingManyRowsLike[A, Repr, TargetingOne[Option[A]]]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return Applied LIMIT (i.e. maximum number of rows returned for each query)
	  */
	def limit: Option[Int]
	/**
	  * @return Applied OFFSET (i.e. how many rows are skipped at the beginning of each query)
	  */
	def offset: Int
	/**
	  * @return Whether, when parsing result rows, the read rows should be tested for duplicate entries.
	  *         If true, duplicate rows (according to the present primary keys) are ignored.
	  */
	def limitsToUniqueIndices: Boolean
	
	/**
	  * @param limit Limit to apply to the SQL query, i.e. the maximum number of rows to return
	  * @return Copy of this access point, applying the specified limit
	  */
	def withLimit(limit: Int): Repr
	/**
	  * @param offset Offset to apply to the SQL query, i.e. the number of rows to skip at the beginning of each query.
	  * @param limit Limit to apply to the SQL query, i.e. the maximum number of rows to return
	  *              (default = currently defined limit)
	  * @return Copy of this access point applying the specified limit and offset
	  */
	def withOffset(offset: Int, limit: Option[Int] = limit): Repr
	/**
	  * @param limit Whether, when parsing result rows, the read rows should be tested for duplicate entries.
	  *              If true, duplicate rows (according to the present primary keys) are ignored.
	  * @return A copy of this access point applying the specified setting
	  */
	def withLimitToUniqueIndices(limit: Boolean): Repr
	
	/**
	  * @param row A row read from the database.
	  *            May be assumed to contain [[selectTarget]].
	  * @return An item parsed from the specified row.
	  *         None if no item was parsed or present.
	  */
	protected def parse(row: Row): Option[A]
	
	
	// IMPLEMENTED  --------------------
	
	// Applies limit and offset
	override protected def finalizeStatement(statement: SqlSegment): SqlSegment = {
		val base = statement + limit.map(Limit.apply)
		offset match {
			case 0 => base
			case o => base + Offset(o)
		}
	}
	override protected def parse(result: ResultStream): Seq[A] = _parse[A](result)(parse)
	override def stream[B](f: Iterator[A] => B)(implicit connection: Connection): B =
		pullWith(toSelect, f(Iterator.empty)) { processStream(_) { rows => f(rows.flatMap(parse)) } }
	
	override def take(n: Int): Repr = withLimit(limit match {
		case Some(limit) => (limit min n) max 0
		case None => n max 0
	})
	override def drop(n: Int): Repr = withOffset(offset + n, limit.map { l => (l - n) max 0 })
	override def slice(range: HasInclusiveEnds[Int]): Repr = withOffset(range.start, Some(range.end - range.start + 1))
	
	override def pullWith[B](column: TableColumn)(map: Value => B)(implicit connection: Connection): Seq[(A, B)] =
		pullManyWith((selectTarget + column).toSelect(target join column.table)) { _parse(_) {
			row => parse(row).map { _ -> map(row(column)) }
		} }
	override def pullWith[B](columns: Seq[TableColumn])(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)] =
		pullManyWith(
			(selectTarget + columns).toSelect(target join columns.iterator.map { _.table }.distinct.toOptimizedSeq)) {
			_parse(_) { row => parse(row).map { _ -> map(columns.map(row.apply)) } }
		}
	override def pullWithMany[B](column: TableColumn)(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)] =
		_pullWithMany(Single(column)) { rows => map(rows.map { _.apply(column) }) }
	override def pullWithMany[B](columns: Seq[TableColumn])(map: Seq[Seq[Value]] => B)
	                            (implicit connection: Connection): Seq[(A, B)] =
		_pullWithMany(columns) { rows => map(rows.map { row => columns.map(row.apply) }) }
	
	
	// OTHER    ----------------------
	
	private def _parse[B](result: ResultStream)(f: Row => Option[B]): Seq[B] =
		processStream(result) { _.flatMap(f).toOptimizedSeq }
	private def processStream[B](result: ResultStream)(f: Iterator[Row] => B) = {
		// Case: Unique indices required => Uses more advanced mapping
		if (limitsToUniqueIndices) {
			val indices = keys
			f(result.rowsIterator
				.map { r => r -> indices.iterator.map(r.apply).caching }.distinctBy { _._2 }.map { _._1 })
		}
		// Case: Non-unique rows allowed => Uses simple row-by-row parsing
		else
			f(result.rowsIterator)
	}
	private def _pullWithMany[B](extraColumns: Seq[TableColumn])(map: Seq[Row] => B)(implicit connection: Connection) = {
		pullManyWith(
			(selectTarget + (extraColumns: SelectTarget))
				.toSelect(target join extraColumns.iterator.map { _.table }.distinct.toOptimizedSeq),
			ordering = None) {
			result =>
				val indices = keys
				/*
				NB: Below implementation doesn't assume that primary elements are formed of consecutive rows,
				but uses more memory.
				
				result.rowsIterator.toOptimizedSeq.groupBy { row => indices.map(row.apply) }.valuesIterator
					.flatMap { rows => parse(rows.head).map { _ -> map(rows) } }
					.toOptimizedSeq
				 */
				result.rowsIterator.groupConsecutiveBy { row => indices.map(row.apply) }
					.flatMap { case (_, rows) => parse(rows.head).map { _ -> map(rows) } }
					.toOptimizedSeq
		}
	}
}

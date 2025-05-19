package utopia.vault.nosql.targeting.many

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.{Column, Result, Row, Table}
import utopia.vault.model.template.Joinable
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{JoinType, Limit, Offset, SqlSegment}

/**
  * Common trait for interfaces that are used for querying multiple row-specific instances from the DB at once,
  * using a FromRowFactory
  * @author Mikko Hilpinen
  * @since 16.05.2025, v1.21
  */
trait AccessManyRowsLike[+A, +Repr] extends AccessManyLike[A, Repr] with TargetingManyRowsLike[A, Repr]
{
	// ABSTRACT ------------------------
	
	def limit: Option[Int]
	def offset: Int
	def limitsToUniqueIndices: Boolean
	
	def withLimit(limit: Int): Repr
	def withOffset(offset: Int, limit: Option[Int] = limit): Repr
	def withLimitToUniqueIndices(limit: Boolean): Repr
	
	def extendTo[B](tables: Seq[Table], exclusiveColumns: Seq[Column] = Empty, bridgingJoins: Seq[Joinable] = Empty,
	                joinType: JoinType = Inner)
	               (f: Seq[(A, Row)] => Seq[B]): AccessMany[B]
	def extendToMany[B](tables: Seq[Table], exclusiveColumns: Seq[Column] = Empty, bridgingJoins: Seq[Joinable] = Empty,
	                    joinType: JoinType = Inner)
	                   (f: Seq[(A, Seq[Row])] => Seq[B]): AccessMany[B]
	
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
	override protected def parse(result: Result): Seq[A] = _parse[A](result)(parse)
	
	override def take(n: Int): Repr = withLimit(limit match {
		case Some(limit) => (limit min n) max 0
		case None => n max 0
	})
	override def drop(n: Int): Repr = withOffset(offset + n, limit.map { l => (l - n) max 0 })
	override def slice(range: HasInclusiveEnds[Int]): Repr = withOffset(range.start, Some(range.end - range.start + 1))
	
	override def pullWith[B](column: Column)(map: Value => B)(implicit connection: Connection): Seq[(A, B)] =
		pullManyWith((selectTarget + column).toSelect(target)) { _parse(_) {
			row => parse(row).map { _ -> map(row(column)) }
		} }
	override def pullWith[B](columns: Seq[Column])(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)] =
		pullManyWith((selectTarget + columns).toSelect(target)) { _parse(_) { row =>
			parse(row).map { _ -> map(columns.map(row.apply)) }
		} }
	override def pullWithMany[B](column: Column)(map: Seq[Value] => B)(implicit connection: Connection): Seq[(A, B)] =
		_pullWithMany(column) { rows => map(rows.map { _.apply(column) }) }
	override def pullWithMany[B](columns: Seq[Column])(map: Seq[Seq[Value]] => B)
	                            (implicit connection: Connection): Seq[(A, B)] =
		_pullWithMany(columns) { rows => map(rows.map { row => columns.map(row.apply) }) }
	
	
	// OTHER    ----------------------
	
	private def _parse[B](result: Result)(f: Row => Option[B]): Seq[B] = {
		// Case: Unique indices required => Uses more advanced mapping
		if (limitsToUniqueIndices) {
			val indices = keys
			result.rows.map { r => r -> indices.iterator.map(r.apply).caching }.iterator.distinctBy { _._2 }
				.flatMap { case (row, _) => f(row) }.toOptimizedSeq
		}
		// Case: Non-unique rows allowed => Uses simple row-by-row parsing
		else
			result.rows.view.flatMap(f).toOptimizedSeq
	}
	private def _pullWithMany[B](extraTarget: SelectTarget)(map: Seq[Row] => B)(implicit connection: Connection) = {
		pullManyWith((selectTarget + extraTarget).toSelect(target), ordering = None) { result =>
			val indices = keys
			result.rows.groupBy { row => indices.map(row.apply) }.valuesIterator
				.flatMap { rows => parse(rows.head).map { _ -> map(rows) } }
				.toOptimizedSeq
		}
	}
}

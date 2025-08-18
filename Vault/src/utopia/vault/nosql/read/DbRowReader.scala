package utopia.vault.nosql.read

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Single}
import utopia.vault.model.enumeration.SelectTarget
import utopia.vault.model.immutable.{Column, Row, Table}
import utopia.vault.nosql.read.DbRowReader.MappingDbRowReader
import utopia.vault.nosql.read.linked.{CombiningDbRowReader, MultiLinkedDbReader, PossiblyCombiningDbRowReader}
import utopia.vault.nosql.read.parse.{ParseRow, ParseRows}
import utopia.vault.sql.SqlTarget

import scala.collection.View
import scala.util.Try

object DbRowReader
{
	// NESTED   -----------------------
	
	private class MappingDbRowReader[-A, +B](reader: DbRowReader[A], f: A => B) extends DbRowReader[B]
	{
		override def selectTarget: SelectTarget = reader.selectTarget
		override def table: Table = reader.table
		override def tables: Seq[Table] = reader.tables
		override def target: SqlTarget = reader.target
		
		override def shouldParse(row: Row): Boolean = reader.shouldParse(row)
		override def apply(row: Row): Try[B] = reader(row).map(f)
	}
}

/**
  * Common trait for classes which target and read data from the database, processing row-specific entries.
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
trait DbRowReader[+A] extends DbReader[Seq[A]] with ParseRow[A]
{
	// OTHER    --------------------------
	
	/**
	 * @param f A mapping function applied to individual parsed items
	 * @tparam B Type of mapping results
	 * @return A reader which maps the individual results of this reader
	 */
	def map[B](f: A => B): DbRowReader[B] = new MappingDbRowReader[A, B](this, f)
	
	/**
	  * Combines this reader with another reader
	  * @param right Reader to join to this one
	  * @param bridges Joins to perform before joining 'right'
	  * @param merge A function for merging the results
	  * @tparam R Type of parsed joined items
	  * @tparam B Type of merge results
	  * @return A reader which combines the results of these two readers
	  * @see [[leftJoin]]
	  */
	def join[R, B](right: DbRowReader[R], bridges: Seq[Table] = Empty)
	              (merge: (A, R) => B): DbRowReader[B] =
		CombiningDbRowReader(this, right, bridges)(merge)
	/**
	  * Combines this reader with another reader
	  * @param right Reader to join to this one
	  * @param bridges Joins to perform before joining 'right'
	  * @param merge A function for merging the results.
	  *              The right side element is provided, if one was parsed.
	  * @tparam R Type of parsed joined items
	  * @tparam B Type of merge results
	  * @return A reader which combines the results of these two readers
	  */
	def leftJoin[R, B](right: DbRowReader[R], bridges: Seq[Table] = Empty)
	                  (merge: (A, Option[R]) => B): DbRowReader[B] =
		PossiblyCombiningDbRowReader(this, right, bridges)(merge)
	/**
	 * Combines this reader with another reader in one-to-many linking
	 * @param right Reader to join to this one
	 * @param bridges Joins to perform before joining 'right'
	 * @param merge A function for merging the results. n right side elements are provided.
	 * @tparam R Type of parsed joined items
	 * @tparam B Type of merge results
	 * @return A reader which combines the results of these two readers
	 */
	def multiJoin[R, B](right: DbRowReader[R], bridges: Seq[Table] = Empty,
	                    neverEmptyRight: Boolean = false)
	                   (merge: (A, Seq[R]) => B): DbReader[Seq[B]] =
		MultiLinkedDbReader(this, right, bridges, neverEmptyRight)(merge)
	
	/**
	 * @param rows Rows to parse
	 * @param f A function which combines a parsed item and the associated rows
	 * @tparam B Type of 'f' results
	 * @return An iterator that yields the combined items
	 */
	def parseMultiLinked[B](rows: IterableOnce[Row])(f: (A, Iterable[Row]) => B) =
		table.primaryColumn match {
			case Some(index) =>
				// Groups the rows by the primary key, then parses the results
				// For buffered collections, uses groupBy
				rows match {
					case rowsView: View[Row] => _parseMultiLinked(rowsView.iterator, index)(f)
					case rows: Iterable[Row] =>
						rows.groupBy { _(index) }.valuesIterator
							.flatMap { rows => tryParse(rows.head).map { left => f(left, rows) } }
					case rows => _parseMultiLinked(rows.iterator, index)(f)
				}
			
			// Case: The primary target's table doesn't contain a primary key (unexpected) => Can't join rows
			case None => rows.iterator.flatMap { row => tryParse(row).map { left => f(left, Single(row)) } }
		}
	/**
	 * @param rows Rows to parse
	 * @param parser Secondary parser to use to parse the linked rows
	 * @param f A function which combines a parsed item and the 'parser's results from the associated rows
	 * @tparam R Type of secondary parsing results
	 * @tparam B Type of 'f' results
	 * @return An iterator that yields the combined items
	 */
	def parseMultiLinkedWith[R, B](rows: IterableOnce[Row], parser: ParseRows[R])(f: (A, R) => B) =
		parseMultiLinked(rows) { (left, rows) => f(left, parser(OptimizedIndexedSeq.from(rows))) }
		
	private def _parseMultiLinked[B](rowsIter: Iterator[Row], index: Column)(f: (A, Iterable[Row]) => B) =
		rowsIter.groupBy { _(index) }.flatMap { case (_, rows) => tryParse(rows.head).map { left => f(left, rows) } }
}
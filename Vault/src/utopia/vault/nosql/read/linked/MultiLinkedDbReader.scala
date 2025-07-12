package utopia.vault.nosql.read.linked

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.vault.model.immutable.{Row, Table}
import utopia.vault.model.mutable.ResultStream
import utopia.vault.nosql.read.DbRowReader
import utopia.vault.nosql.read.parse.ParseRows
import utopia.vault.sql.JoinType
import utopia.vault.sql.JoinType.Inner

object MultiLinkedDbReader
{
	// OTHER    ----------------------------
	
	/**
	 * Creates a new DB reader by combining two other readers in a one-to-many fashion
	 * @param left The primary reader
	 * @param right The secondary reader
	 * @param bridges Joins to perform between the left and the right target (default = empty)
	 * @param neverEmptyRight Whether to never expect a one-to-none connection.
	 *                        If true, inner joining will be used.
	 *                        Default = false = Left join will be used in order to accommodate the one-to-none case.
	 * @param f A function which accepts the left / primary item, and n right / secondary items, and combines them
	 * @tparam L Type of parsed left items
	 * @tparam R Type of parsed right items
	 * @tparam A Type of combined items
	 * @return A new database reader
	 */
	def apply[L, R, A](left: DbRowReader[L], right: DbRowReader[R],
	                   bridges: Seq[Table] = Empty, neverEmptyRight: Boolean = false)
	                  (f: (L, Seq[R]) => A): MultiLinkedDbReader[L, R, A] =
		new _MultiLinkedDbReader[L, R, A](left, right, bridges, f, neverEmptyRight)
	
	
	// NESTED   ----------------------------
	
	private class _MultiLinkedDbReader[L, R, A](left: DbRowReader[L], right: DbRowReader[R],
	                                            bridges: Seq[Table], f: (L, Seq[R]) => A, neverEmptyRight: Boolean)
		extends MultiLinkedDbReader[L, R, A](left, right, bridges, neverEmptyRight)
	{
		override protected def combine(left: L, right: Seq[R]): A = f(left, right)
	}
}

/**
 * A database reader interface which handles one-to-many links,
 * delegating parsing to two other readers.
 *
 * @author Mikko Hilpinen
 * @since 10.07.2025, v1.22
 */
abstract class MultiLinkedDbReader[L, R, A](left: DbRowReader[L], right: DbRowReader[R],
                                   bridges: Seq[Table] = Empty, neverEmptyRight: Boolean = false)
	extends JoiningDbReader[L, R, A](left, right, bridges, if (neverEmptyRight) Inner else JoinType.Left)
		with ParseRows[Seq[A]]
{
	// ABSTRACT ----------------------------
	
	/**
	 * Combines the parsed items
	 * @param left The left-side item
	 * @param right The linked right-side items
	 * @return The combined item
	 */
	protected def combine(left: L, right: Seq[R]): A
	
	
	// IMPLEMENTED  ------------------------
	
	override def apply(rows: Seq[Row]): Seq[A] = left.parseMultiLinkedWith(rows, right)(combine).toOptimizedSeq
	
	override def apply(stream: ResultStream): Seq[A] =
		left.parseMultiLinkedWith(stream.rowsIterator, right)(combine).toOptimizedSeq
}

package utopia.vault.nosql.read

import utopia.flow.collection.immutable.Empty
import utopia.vault.model.template.{HasTablesAsTarget, Joinable}
import utopia.vault.nosql.read.linked.{CombiningDbRowReader, PossiblyCombiningDbRowReader}
import utopia.vault.nosql.read.parse.ParseRow

/**
  * Common trait for classes which target and read data from the database, processing row-specific entries.
  * @author Mikko Hilpinen
  * @since 10.07.2025, v1.22
  */
trait DbRowReader[+A] extends DbReader[Seq[A]] with ParseRow[A]
{
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
	def join[R, B](right: DbRowReader[R] with HasTablesAsTarget, bridges: Seq[Joinable] = Empty)
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
	def leftJoin[R, B](right: DbRowReader[R] with HasTablesAsTarget, bridges: Seq[Joinable] = Empty)
	                  (merge: (A, Option[R]) => B): DbRowReader[B] =
		PossiblyCombiningDbRowReader(this, right, bridges)(merge)
}
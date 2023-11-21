package utopia.flow.collection.immutable

import utopia.flow.operator.MaybeEmpty

import scala.collection.IndexedSeqView

/**
 * Common trait for 2-dimensional grid data sets
 * @author Mikko Hilpinen
 * @since 22.1.2023, v2.0
 * @tparam A Type of cell values in this matrix
  * @tparam C Type of generic matrix collection returned by map operations
  * @tparam Repr Type of this matrix implementation
 */
trait MatrixLike[+A, +C[_], +Repr] extends utopia.flow.collection.template.MatrixLike[A, Repr] with MaybeEmpty[Repr]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Columns of this matrix, from left to right
	  */
	def columns: IndexedSeq[IndexedSeq[A]]
	/**
	  * @return Rows of this matrix, from top to bottom
	  */
	def rows: IndexedSeq[IndexedSeq[A]]
	
	/**
	 * @return A transposed copy of this matrix.
	 *         In a transposed matrix, the original rows become columns and columns become rows.
	 */
	def transpose: Repr
	
	/**
	  * @param f A mapping function that accepts and returns a set of columns.
	  *          NB: The mapping function must return values with uniform length.
	  * @tparam B Type of cell values in the returned columns
	  * @return A copy of this matrix with mapped columns
	  */
	def mapColumns[B](f: IndexedSeqView[A] => IterableOnce[B]): C[B]
	/**
	  * @param f A mapping function that accepts and returns a set of rows.
	  *          NB: The mapping function must return values with uniform length.
	  * @tparam B Type of cell values in the returned rows
	  * @return A copy of this matrix with mapped rows
	  */
	def mapRows[B](f: IndexedSeqView[A] => IterableOnce[B]): C[B]
	/**
	  * @param f A mapping function that accepts a column and its index, and returns a modified column.
	  *          NB: The mapping function must return values with uniform length.
	  * @tparam B Type of cell values in the returned columns
	  * @return A copy of this matrix with mapped columns
	  */
	def mapColumnsWithIndex[B](f: (IndexedSeqView[A], Int) => IterableOnce[B]): C[B]
	/**
	  * @param f A mapping function that accepts a row and its index, and returns a modified row.
	  *          NB: The mapping function must return values with uniform length.
	  * @tparam B Type of cell values in the returned rows
	  * @return A copy of this matrix with mapped rows
	  */
	def mapRowsWithIndex[B](f: (IndexedSeqView[A], Int) => IterableOnce[B]): C[B]
	
	/**
	  * @param f A mapping function for transforming cell values
	  * @tparam B New type of cell values
	  * @return A mapped copy of this matrix
	  */
	def map[B](f: A => B): C[B]
	/**
	  * @param f A mapping function for transforming cell values.
	  *          Accepts a cell's value and index, returns the new cell value.
	  * @tparam B New type of cell values
	  * @return A mapped copy of this matrix
	  */
	def mapWithIndex[B](f: (A, Pair[Int]) => B): C[B]
	
	/**
	  * @param f A mapping function for transforming cell values
	  * @tparam B New type of cell values
	  * @return A lazily mapped copy of this matrix
	  */
	def lazyMap[B](f: A => B): C[B]
	/**
	  * @param f A mapping function for transforming cell values.
	  *          Accepts a cell's value and index, returns the new cell value.
	  * @tparam B New type of cell values
	  * @return A lazily mapped copy of this matrix
	  */
	def lazyMapWithIndex[B](f: (A, Pair[Int]) => B): C[B]
	
	
	// IMPLEMENTED  -----------------------
	
	override def isEmpty: Boolean = columnsView.headOption.forall { _.isEmpty }
	
	override def apply(column: Int, row: Int) = columnsView(column)(row)
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param x Targeted x-coordinate
	  * @throws IndexOutOfBoundsException If the specified coordinate is out of range
	  * @return Column in this matrix that corresponds with the specified x-coordinate
	  */
	@throws[IndexOutOfBoundsException]("If the specified index is out of range")
	def column(x: Int) = columns(x)
	/**
	  * @param y Targeted y-coordinate
	  * @throws IndexOutOfBoundsException If the specified coordinate is out of range
	  * @return Column in this matrix that corresponds with the specified y-coordinate
	  */
	@throws[IndexOutOfBoundsException]("If the specified index is out of range")
	def row(y: Int) = rows(y)
}
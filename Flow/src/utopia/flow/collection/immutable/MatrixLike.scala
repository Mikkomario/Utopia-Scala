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
	
	/**
	  * @param center Targeted center point
	  * @param range Maximum range for the included points / indices in this matrix.
	  *              Default = 1.
	  * @param onlyOrthogonal Whether range calculation should use orthogonal connections instead of
	  *                       diagonal connections.
	  *                       Two matrix cells are orthogonally adjacent when they share an edge.
	  *                       Two cells are diagonally adjacent when they share a point.
	  *                       Default = false = include diagonal connections.
	  * @return An iterator that returns the cell indices around the specified central point.
	  *         The specified point is excluded from this collection / result.
	  */
	def indicesAroundIterator(center: Pair[Int], range: Int = 1, onlyOrthogonal: Boolean = false) = {
		// Collects all indices around the specified spot (but not including that spot)
		val aroundIterator = center
			.mergeWith(size) { (center, length) =>
				val start = (center - range) max 0
				val end = (center + range) min (length - 1)
				if (start > end)
					Empty
				else
					start to end
			}
			.merge { (xRange, yRange) =>
				if (xRange.isEmpty || yRange.isEmpty)
					Iterator.empty
				else
					xRange.iterator.flatMap { x =>
						val iterator = yRange.iterator.map { y => Pair(x, y) }
						if (x == center.first)
							iterator.filterNot { _ == center }
						else
							iterator
					}
			}
		// May limit the results to points at certain orthogonal distance
		if (onlyOrthogonal)
			aroundIterator.filter { _.mergeWith(center) { (p, c) => (p - c).abs }.sum <= range }
		else
			aroundIterator
	}
}
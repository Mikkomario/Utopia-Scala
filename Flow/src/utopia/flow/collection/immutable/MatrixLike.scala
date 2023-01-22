package utopia.flow.collection.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.MaybeEmpty

/**
 * Common trait for 2-dimensional grid data sets
 * @author Mikko Hilpinen
 * @since 22.1.2023, v2.0
 * @tparam A Type of cell values in this matrix
  * @tparam Repr Type of this matrix implementation
 */
trait MatrixLike[+A, +Repr] extends MaybeEmpty[Repr]
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
	  * @return An empty copy of this matrix
	  */
	protected def empty: Repr
	
	/**
	  * @param area Viewed area, as an x-range (for columns) and then an y-range (for rows)
	  * @return A view of this matrix that covers only the specified sub-region.
	  *         Non-overlapping part (i.e. the targeted area outside of this matrix) of the area is not included.
	  */
	def view(area: Pair[NumericSpan[Int]]): Repr
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return The width of this matrix in number of cells
	 */
	def width = columns.size
	/**
	 * @return The height of this matrix in number of cells
	 */
	def height = rows.size
	/**
	 * @return The size of this matrix, counting the number of cells in rows and columns
	 */
	def size = Pair(width, height)
	
	/**
	 * @return An iterator that returns all cell values in this matrix
	 */
	def iterator = indexIterator.map(apply)
	/**
	 * @return An iterator that returns all the cell (x, y) indices within this matrix.
	 */
	def indexIterator =
		rows.indices.iterator.flatMap { y => columns.indices.iterator.map { x => Pair(x, y) } }
	/**
	 * @return An iterator that returns all cell values, paired with their indices, within this matrix.
	 */
	def iteratorWithIndex = indexIterator.map { i => apply(i) -> i }
	
	
	// IMPLEMENTED  -----------------------
	
	override def isEmpty: Boolean = columns.headOption.forall { _.isEmpty }
	
	
	// OTHER    ---------------------------
	
	/**
	 * Reads the value of a single cell in this matrix
	 * @param column Targeted column index [0, width[, i.e. the x-coordinate
	 * @param row Targeted row index [0, height[, i.e. the y-coordinate
	 * @return The value of this matrix at the targeted cell
	 * @throws IndexOutOfBoundsException If specified column or row index is out of range
	 */
	@throws[IndexOutOfBoundsException]("If specified column or row index is out of range")
	def apply(column: Int, row: Int) = columns(column)(row)
	/**
	 * Reads the value of a single cell in this matrix
	 * @param coordinate The targeted x-y-coordinate, as a pair
	 * @return The value of this matrix at the targeted cell
	 * @throws IndexOutOfBoundsException If the specified coordinate is outside of this matrix
	 */
	@throws[IndexOutOfBoundsException]("If coordinate is outside of this matrix")
	def apply(coordinate: Pair[Int]): A = apply(coordinate.first, coordinate.second)
	
	/**
	 * Reads the value of a single cell in this matrix
	 * @param column Targeted column index [0, width[, i.e. the x-coordinate
	 * @param row    Targeted row index [0, height[, i.e. the y-coordinate
	 * @return The value of this matrix at the targeted cell. None if that cell is out of range.
	 */
	def getOption(column: Int, row: Int) = columns.getOption(column).flatMap { _.getOption(row) }
	/**
	 * Reads the value of a single cell in this matrix
	 * @param coordinate The targeted x-y-coordinate, as a pair
	 * @return The value of this matrix at the targeted cell. None if that coordinate is outside of this matrix.
	 */
	def getOption(coordinate: Pair[Int]): Option[A] = getOption(coordinate.first, coordinate.second)
	
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
	 * @param minX Smallest included x-coordinate
	 * @param maxX Largest included x-coordinate
	 * @param minY Smallest included y-coordinate
	 * @param maxY Largest included y-coordinate
	 * @return A view of this matrix that covers only the specified sub-region.
	 *         Non-overlapping part (i.e. the targeted area outside of this matrix) of the area is not included.
	 */
	def viewBetween(minX: Int, maxX: Int, minY: Int, maxY: Int) =
		view(Pair(NumericSpan(minX, maxX), NumericSpan(minY, maxY)))
	/**
	 * @param points The minimum (top left) and the maximum (bottom right) coordinates of the targeted sub-region
	 * @return A view of this matrix that covers only the specified sub-region.
	 *         Non-overlapping part (i.e. the targeted area outside of this matrix) of the area is not included.
	 */
	def viewBetween(points: Pair[Pair[Int]]): Repr =
		viewBetween(points.first.first, points.second.first, points.first.second, points.second.second)
	
	/**
	 * @param span Targeted horizontal span
	 * @return A sub-region of this matrix that only contains the specified columns
	 */
	def sliceColumns(span: NumericSpan[Int]) = view(Pair(span, NumericSpan(0, height - 1)))
	/**
	 * @param span Targeted vertical span
	 * @return A sub-region of this matrix that only contains the specified rows
	 */
	def sliceRows(span: NumericSpan[Int]) = view(Pair(NumericSpan(0, width - 1), span))
	/**
	 * @param size Number of columns (first) and rows (second) to include (from the top left)
	 * @return A sub-region starting from the top-left corner of this matrix and covering up to the specified size
	 */
	def take(size: Pair[Int]) = {
		if (size.exists { _ <= 0 })
			empty
		else
			view(size.map { len => NumericSpan(0, len - 1) })
	}
	/**
	 * @param width Number of columns to include, from the left
	 * @return Targeted sub-region of this matrix
	 */
	def takeColumns(width: Int) = take(Pair(width, height))
	/**
	 * @param height Number of rows to include, from the top
	 * @return Targeted sub-region of this matrix
	 */
	def takeRows(height: Int) = take(Pair(width, height))
	/**
	 * @param size Amount of columns (first) and rows (second) to exclude from this matrix (from the top left)
	 * @return A sub-region of this matrix where the specified area is excluded
	 */
	def drop(size: Pair[Int]) = {
		val s = this.size
		if (size.existsWith(s) { _ >= _ })
			empty
		else
			view(size.mergeWith(s) { (start, max) => NumericSpan(start, max) })
	}
	/**
	 * @param width Number of columns to exclude, from the left
	 * @return A sub-region of this matrix containing the remaining columns
	 */
	def dropColumns(width: Int) = drop(Pair(width, 0))
	/**
	 * @param height Number of rows to exclude, from the top
	 * @return A sub-region of this matrix containing the remaining rows
	 */
	def dropRows(height: Int) = drop(Pair(0, height))
	
	/**
	 * @param f A mapping function that accepts and returns a set of columns.
	 *          NB: The mapping function must return values with uniform length.
	 * @tparam B Type of cell values in the returned columns
	 * @return A copy of this matrix with mapped columns
	 */
	def mapColumns[B](f: IndexedSeq[A] => IndexedSeq[B]) = Matrix.withColumns(columns.map(f))
	/**
	 * @param f A mapping function that accepts and returns a set of rows.
	 *          NB: The mapping function must return values with uniform length.
	 * @tparam B Type of cell values in the returned rows
	 * @return A copy of this matrix with mapped rows
	 */
	def mapRows[B](f: IndexedSeq[A] => IndexedSeq[B]) = Matrix.withRows(rows.map(f))
	/**
	 * @param f A mapping function that accepts a column and its index, and returns a modified column.
	 *          NB: The mapping function must return values with uniform length.
	 * @tparam B Type of cell values in the returned columns
	 * @return A copy of this matrix with mapped columns
	 */
	def mapColumnsWithIndex[B](f: (IndexedSeq[A], Int) => IndexedSeq[B]) =
		Matrix.withColumns(columns.zipWithIndex.map { case (col, x) => f(col, x) })
	/**
	 * @param f A mapping function that accepts a row and its index, and returns a modified row.
	 *          NB: The mapping function must return values with uniform length.
	 * @tparam B Type of cell values in the returned rows
	 * @return A copy of this matrix with mapped rows
	 */
	def mapRowsWithIndex[B](f: (IndexedSeq[A], Int) => IndexedSeq[B]) =
		Matrix.withRows(rows.zipWithIndex.map { case (row, y) => f(row, y) })
	
	/**
	 * @param f A mapping function for transforming cell values
	 * @tparam B New type of cell values
	 * @return A mapped copy of this matrix
	 */
	def map[B](f: A => B) = mapColumns { _.map(f) }
	/**
	 * @param f A mapping function for transforming cell values.
	 *          Accepts a cell's value and index, returns the new cell value.
	 * @tparam B New type of cell values
	 * @return A mapped copy of this matrix
	 */
	def mapWithIndex[B](f: (A, Pair[Int]) => B) =
		mapColumnsWithIndex[B] { (col, x) => col.zipWithIndex.map { case (item, y) => f(item, Pair(x, y)) } }
	
	/**
	 * @param f A mapping function for transforming cell values
	 * @tparam B New type of cell values
	 * @return A lazily mapped copy of this matrix
	 */
	def lazyMap[B](f: A => B) = Matrix.withColumns(columns.lazyMap { _.lazyMap(f) })
	/**
	 * @param f A mapping function for transforming cell values.
	 *          Accepts a cell's value and index, returns the new cell value.
	 * @tparam B New type of cell values
	 * @return A lazily mapped copy of this matrix
	 */
	def lazyMapWithIndex[B](f: (A, Pair[Int]) => B) =
		Matrix.withColumns(columns.zipWithIndex.lazyMap { case (col, x) =>
			col.zipWithIndex.lazyMap { case (item, y) => f(item, Pair(x, y)) } })
}
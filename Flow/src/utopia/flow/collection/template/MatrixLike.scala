package utopia.flow.collection.template

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.Sign
import utopia.flow.operator.Sign.{Negative, Positive}

import scala.collection.IndexedSeqView

/**
 * Common trait for immutable and mutable 2-dimensional grid data sets
 * @author Mikko Hilpinen
 * @since 22.1.2023, v2.0
 * @tparam A Type of cell values in this matrix
  * @tparam V Type of view to this matrix
 */
trait MatrixLike[+A, +V] extends PartialFunction[Pair[Int], A]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The width of this matrix in number of cells
	  */
	def width: Int
	/**
	  * @return The height of this matrix in number of cells
	  */
	def height: Int
	
	/**
	 * @return Columns of this matrix, from left to right
	 */
	def columnsView: IndexedSeqView[IndexedSeqView[A]]
	/**
	 * @return Rows of this matrix, from top to bottom
	 */
	def rowsView: IndexedSeqView[IndexedSeqView[A]]
	
	/**
	  * @return An empty copy of or view into this matrix
	  */
	protected def empty: V
	
	/**
	  * Reads the value of a single cell in this matrix
	  * @param column Targeted column index [0, width[, i.e. the x-coordinate
	  * @param row    Targeted row index [0, height[, i.e. the y-coordinate
	  * @return The value of this matrix at the targeted cell
	  * @throws IndexOutOfBoundsException If specified column or row index is out of range
	  */
	@throws[IndexOutOfBoundsException]("If specified column or row index is out of range")
	def apply(column: Int, row: Int): A
	/**
	  * @param area Viewed area, as an x-range (for columns) and then an y-range (for rows)
	  * @return A view of this matrix that covers only the specified sub-region.
	  *         Non-overlapping part (i.e. the targeted area outside of this matrix) of the area is not included.
	  */
	def view(area: Pair[NumericSpan[Int]]): V
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return The size of this matrix, counting the number of cells in rows and columns
	 */
	def size = Pair(width, height)
	
	/**
	 * @return Valid column indices within this matrix
	 */
	def columnIndices = 0 until width
	/**
	 * @return Valid row indices within this matrix
	 */
	def rowIndices = 0 until height
	
	/**
	 * @return An iterator that returns all cell values in this matrix
	 */
	def iterator = indexIterator.map(apply)
	/**
	 * @return An iterator that returns all the cell (x, y) indices within this matrix.
	 */
	def indexIterator =
		(0 until height).iterator.flatMap { y => (0 until width).iterator.map { x => Pair(x, y) } }
	/**
	 * @return An iterator that returns all cell values, paired with their indices, within this matrix.
	 */
	def iteratorWithIndex = indexIterator.map { i => apply(i) -> i }
	
	
	// IMPLEMENTED  -----------------------
	
	override def isDefinedAt(x: Pair[Int]): Boolean = x.forallWith(size) { (p, len) => p >= 0 && p < len }
	
	/**
	  * Reads the value of a single cell in this matrix
	  * @param coordinate The targeted x-y-coordinate, as a pair
	  * @return The value of this matrix at the targeted cell
	  * @throws IndexOutOfBoundsException If the specified coordinate is outside of this matrix
	  */
	@throws[IndexOutOfBoundsException]("If coordinate is outside of this matrix")
	def apply(coordinate: Pair[Int]): A = apply(coordinate.first, coordinate.second)
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param axis Targeted axis, where Negative is the X-axis (rows) and Positive is the Y-axis (columns)
	 * @return A view into either the rows or the columns of this matrix
	 */
	def viewLinesAlong(axis: Sign) = axis match {
		case Negative => rowsView
		case Positive => columnsView
	}
	
	/**
	 * Reads the value of a single cell in this matrix
	 * @param column Targeted column index [0, width[, i.e. the x-coordinate
	 * @param row    Targeted row index [0, height[, i.e. the y-coordinate
	 * @return The value of this matrix at the targeted cell. None if that cell is out of range.
	 */
	def getOption(column: Int, row: Int): Option[A] = getOption(Pair(column, row))
	/**
	 * Reads the value of a single cell in this matrix
	 * @param coordinate The targeted x-y-coordinate, as a pair
	 * @return The value of this matrix at the targeted cell. None if that coordinate is outside of this matrix.
	 */
	def getOption(coordinate: Pair[Int]): Option[A] = lift(coordinate)
	
	/**
	 * @param x Targeted x-coordinate
	 * @throws IndexOutOfBoundsException If the specified coordinate is out of range
	 * @return Column in this matrix that corresponds with the specified x-coordinate
	 */
	@throws[IndexOutOfBoundsException]("If the specified index is out of range")
	def viewColumn(x: Int) = columnsView(x)
	/**
	 * @param y Targeted y-coordinate
	 * @throws IndexOutOfBoundsException If the specified coordinate is out of range
	 * @return Column in this matrix that corresponds with the specified y-coordinate
	 */
	@throws[IndexOutOfBoundsException]("If the specified index is out of range")
	def viewRow(y: Int) = rowsView(y)
	
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
	def viewBetween(points: Pair[Pair[Int]]): V =
		viewBetween(points.first.first, points.second.first, points.first.second, points.second.second)
	
	/**
	  * Views a region within this matrix
	  * @param center The center of the region
	  * @param radius Radius of the region (horizontally & vertically)
	  * @return A view into the specified region within this matrix
	  */
	def viewRegionAround(center: Pair[Int], radius: Pair[Int] = Pair.twice(1)) =
		view(center.mergeWith(radius) { (p, r) => NumericSpan(p - r, p + r) })
	/**
	  * Views a square region within this matrix
	  * @param center The center of the region
	  * @param radius Radius of the region
	  * @return A view into the specified region within this matrix
	  */
	def viewRegionAround(center: Pair[Int], radius: Int): V = viewRegionAround(center, Pair.twice(radius))
	
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
	  * Crops the specified amount on all sides
	  * @param amount Amount of cells to crop from the sides
	  * @return A cropped view into this matrix
	  */
	def crop(amount: Pair[Int]) = {
		val s = size
		if (s.existsWith(amount) { _ <= _ * 2 })
			empty
		else
			view(s.mergeWith(amount) { (len, amount) =>NumericSpan(amount, len - amount - 1) })
	}
	/**
	  * Crops the specified amount on all sides
	  * @param amount Amount of cells to crop from the sides
	  * @return A cropped view into this matrix
	  */
	def crop(amount: Int): V = crop(Pair.twice(amount))
}
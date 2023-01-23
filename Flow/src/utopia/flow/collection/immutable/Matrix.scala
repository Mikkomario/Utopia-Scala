package utopia.flow.collection.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

object Matrix
{
	// ATTRIBUTES   --------------------------
	
	/**
	 * An empty matrix
	 */
	lazy val empty: Matrix[Nothing] = new EmptyMatrix()
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param cols A set of columns, from left to right.
	 *             NB: Each column must have the same length.
	 * @tparam A Type of cell values
	 * @return A new matrix consisting of those columns
	 */
	def withColumns[A](cols: IndexedSeq[IndexedSeq[A]]): Matrix[A] = {
		cols.headOption match {
			case Some(firstCol) =>
				val height = firstCol.size
				new _Matrix[A](View.fixed(cols), Lazy { (0 until height).map { y => cols.map { _(y) } } })
			case None => empty
		}
	}
	/**
	 * @param rows A set of rows, from top to bottom.
	 *             NB: Each column must have the same length.
	 * @tparam A Type of cell values
	 * @return A new matrix consisting of those rows
	 */
	def withRows[A](rows: IndexedSeq[IndexedSeq[A]]): Matrix[A] = {
		rows.headOption match {
			case Some(firstRow) =>
				val width = firstRow.size
				new _Matrix[A](Lazy { (0 until width).map { x => rows.map { _(x) } } }, View.fixed(rows))
			case None => empty
		}
	}
	
	/**
	 * Fills a matrix
	 * @param size The size of the resulting matrix
	 * @param f    A function for filling each cell. Accepts the cell coordinate as a Pair.
	 * @tparam A Type of cell values
	 * @return A new matrix
	 */
	def fill[A](size: Pair[Int])(f: Pair[Int] => A) =
		withColumns((0 until size.first).map { x =>
			(0 until size.second).map { y => f(Pair(x, y)) }
		})
	/**
	 * Lazily fills a matrix
	 * @param size The size of the resulting matrix
	 * @param f A function for filling each cell. Accepts the cell coordinate as a Pair.
	 * @tparam A Type of cell values
	 * @return A new matrix
	 */
	def lazyFill[A](size: Pair[Int])(f: Pair[Int] => A) =
		withColumns((0 until size.first).lazyMap { x =>
			(0 until size.second).lazyMap { y => f(Pair(x, y)) }
		})
	
	
	// NESTED   ------------------------------
	
	private class EmptyMatrix[+A] extends Matrix[A]
	{
		override val columns: IndexedSeq[IndexedSeq[A]] = Vector()
		override val rows: IndexedSeq[IndexedSeq[A]] = Vector()
		
		override def transpose: Matrix[A] = this
		
		override def view(area: Pair[NumericSpan[Int]]) = this
	}
	
	private class _Matrix[A](colView: View[IndexedSeq[IndexedSeq[A]]], rowView: View[IndexedSeq[IndexedSeq[A]]])
		extends Matrix[A]
	{
		override def columns: IndexedSeq[IndexedSeq[A]] = colView.value
		override def rows: IndexedSeq[IndexedSeq[A]] = rowView.value
		
		override def transpose: Matrix[A] = new _Matrix[A](rowView, colView)
	}
}

/**
 * A 2-dimensional grid data set
 * @author Mikko Hilpinen
 * @since 22.1.2023, v2.0
 * @tparam A Type of cell values in this matrix
 */
trait Matrix[+A] extends MatrixLike[A, Matrix[A]]
{
	// IMPLEMENTED  -----------------------
	
	override protected def empty: Matrix[A] = Matrix.empty
	
	override def self: Matrix[A] = this
	
	override def transpose: Matrix[A] = new Matrix._Matrix[A](Lazy { rows }, Lazy { columns })
	
	/**
	  * @param area Viewed area, as an x-range (for columns) and then an y-range (for rows)
	  * @return A view of this matrix that covers only the specified sub-region.
	  *         Non-overlapping part (i.e. the targeted area outside of this matrix) of the area is not included.
	  */
	def view(area: Pair[NumericSpan[Int]]) = MatrixView(this, area)
}
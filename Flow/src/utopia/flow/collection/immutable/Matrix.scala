package utopia.flow.collection.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Matrix.MatrixView
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
				new _Matrix[A](View.fixed(cols), Lazy { (0 until height).map(cols.apply) })
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
				new _Matrix[A](Lazy { (0 until width).map(rows.apply) }, View.fixed(rows))
			case None => empty
		}
	}
	
	
	// NESTED   ------------------------------
	
	private class EmptyMatrix[+A] extends Matrix[A]
	{
		override val columns: IndexedSeq[IndexedSeq[A]] = Vector()
		override val rows: IndexedSeq[IndexedSeq[A]] = Vector()
		
		override def transpose: Matrix[A] = this
		
		override def view(area: Pair[NumericSpan[Int]]) = this
	}
	
	private class MatrixView[+A](matrix: Matrix[A], area: Pair[NumericSpan[Int]]) extends Matrix[A]
	{
		// ATTRIBUTES   --------------------
		
		override lazy val width = endX - startX
		override lazy val height = endY - startY
		
		override lazy val columns = matrix.columns.slice(area.second).lazyMap { _.slice(area.first) }
		override lazy val rows = matrix.rows.slice(area.first).lazyMap { _.slice(area.second) }
		
		
		// COMPUTED ------------------------
		
		private def startX = area.first.start
		private def endX = area.first.end
		private def startY = area.second.start
		private def endY = area.second.end
		
		
		// IMPLEMENTED  --------------------
		
		override def transpose: Matrix[A] = new _Matrix[A](Lazy { rows }, Lazy { columns })
		
		override def iteratorWithIndex =
			area.second.iterator.flatMap { y =>
				area.first.iterator.map { x => (matrix(x, y), Pair(x - startX, y - startY)) } }
		override def apply(column: Int, row: Int) = matrix(startX + column, startY + row)
		
		override def take(size: Pair[Int]): Matrix[A] = {
			if (size.exists { _ <= 0 })
				empty
			else
				new MatrixView(matrix, area.mergeWith(size) { (span, len) => span.withMaxLength(len) })
		}
		override def drop(size: Pair[Int]): Matrix[A] = {
			if (size.zip(area).exists { case (len, span) => len >= span.length })
				empty
			else
				new MatrixView(matrix, area.mergeWith(size) { (span, len) => span.withStart(span.end - len) })
		}
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
	
	/**
	  * @param area Viewed area, as an x-range (for columns) and then an y-range (for rows)
	  * @return A view of this matrix that covers only the specified sub-region.
	  *         Non-overlapping part (i.e. the targeted area outside of this matrix) of the area is not included.
	  */
	def view(area: Pair[NumericSpan[Int]]): Matrix[A] = {
		val s = size
		if (area.existsWith(s) { (span, len) => span.end < 0 || span.start > len })
			Matrix.empty
		else
			new MatrixView[A](this, area.mergeWith(s) { (span, len) =>
				NumericSpan(span.start max 0, span.end.min(len - 1))
			})
	}
}
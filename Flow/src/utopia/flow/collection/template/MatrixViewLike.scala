package utopia.flow.collection.template

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.NumericSpan

/**
  * Common trait for matrix view implementations
  * @author Mikko Hilpinen
  * @since 23.1.2023, v2.0
  * @tparam A Type of matrix cell values
  * @tparam O Type of values in the viewed matrix
  * @tparam M Type of this view as a matrix
  */
trait MatrixViewLike[+A, O, +M] extends MatrixLike[A, M]
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return The matrix viewed through this view
	  */
	protected def matrix: MatrixLike[O, _]
	/**
	  * @return A mapping function applied to the viewed matrix values.
	  *         Either Left: A simple mapping function, or
	  *         Right: An index-based mapping function that receives both the original value
	  *         and its index within this view
	  */
	protected def viewFunction: Either[O => A, (O, Pair[Int]) => A]
	
	/**
	  * @return The viewed sub-region of the viewed matrix.
	  *         First the horizontal view area and then the vertical view area.
	  */
	def viewArea: Pair[NumericSpan[Int]]
	
	/**
	  * @param area A new view area that lies within the viewed matrix
	  * @return A copy of this view with the specified view area
	  */
	protected def withViewArea(area: Pair[NumericSpan[Int]]): M
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return The horizontal view range (viewed column indices),
	  *         relative to the viewed matrix's coordinate system.
	  */
	def horizontalViewRange = viewArea.first
	/**
	  * @return The vertical view range (viewed row indices),
	  *         relative to the viewed matrix's coordinate system.
	  */
	def verticalViewRange = viewArea.second
	
	/**
	  * @return The first viewed column index
	  */
	def startX = horizontalViewRange.start
	/**
	  * @return The last viewed column index
	  */
	def endX = horizontalViewRange.end
	/**
	  * @return The first viewed row index
	  */
	def startY = verticalViewRange.start
	/**
	  * @return The last viewed row index
	  */
	def endY = verticalViewRange.end
	
	/**
	  * @return View to the columns in the original matrix
	  */
	protected def originalColumnsView =
		matrix.columnsView.slice(horizontalViewRange.start, horizontalViewRange.end + 1)
			.map { _.slice(verticalViewRange.start, verticalViewRange.end + 1) }
	/**
	  * @return View to the rows in the original matrix
	  */
	protected def originalRowsView =
		matrix.rowsView.slice(verticalViewRange.start, verticalViewRange.end + 1)
			.map { _.slice(horizontalViewRange.start, horizontalViewRange.end + 1) }
	
	
	// IMPLEMENTED  ------------------------
	
	override def width = horizontalViewRange.length + 1
	override def height = verticalViewRange.length + 1
	
	override def columnsView = viewFunction match {
		case Left(f) => originalColumnsView.map { _.map(f) }
		case Right(f) =>
			(0 until width).view.map { x => (0 until height).view.map { y => f(original(x, y), Pair(x, y)) } }
	}
	override def rowsView = viewFunction match {
		case Left(f) => originalRowsView.map { _.map(f) }
		case Right(f) =>
			(0 until height).view.map { y => (0 until width).view.map { x => f(original(x, y), Pair(x, y)) } }
	}
	
	override def apply(column: Int, row: Int) = {
		val value = original(column, row)
		viewFunction match {
			case Left(f) => f(value)
			case Right(f) => f(value, Pair(column, row))
		}
	}
	
	override def view(area: Pair[NumericSpan[Int]]): M = viewArea.findMergeWith(area) { _ overlapWith _ } match {
		case Some(overlap) => withViewArea(overlap)
		case None => empty
	}
	
	override def take(size: Pair[Int]) = {
		if (size.exists { _ <= 0 })
			empty
		else
			withViewArea(viewArea.mergeWith(size) { (span, len) => span.withMaxLength(len) })
	}
	override def drop(size: Pair[Int]) = {
		if (size.zip(viewArea).exists { case (len, span) => len >= span.length })
			empty
		else
			withViewArea(viewArea.mergeWith(size) { (span, len) => span.withStart(span.end - len) })
	}
	
	
	// OTHER    --------------------------
	
	/**
	  * @param column Targeted column index (x)
	  * @param row Targeted row index (y)
	  * @return Cell value of the viewed matrix at that location
	  */
	protected def original(column: Int, row: Int) = matrix(startX + column, startY + row)
	/**
	  * @param point Targeted point within this matrix view
	  * @return Cell value of the viewed matrix at that location
	  */
	protected def original(point: Pair[Int]) = matrix(viewArea.mergeWith(point) { _.start + _ })
	
	/**
	  * @param amount Amount to shift this view horizontally (first) and vertically (second)
	  * @return A shifted copy of this view
	  */
	def shiftedBy(amount: Pair[Int]) =
		viewArea.mergeWith(amount) { _ shiftedBy _ }.findMergeWith(matrix.size) { (span, len) =>
			if (span.end < 0 || span.start >= len)
				None
			else
				Some(NumericSpan(span.start max 0, span.end min (len - 1)))
		} match {
			case Some(area) => withViewArea(area)
			case None => empty
		}
	
	/**
	  * @param horizontally Amount to shift this view horizontally
	  * @param vertically Amount to shift this view vertically
	  * @return A shifted copy of this view
	  */
	def shiftedBy(horizontally: Int, vertically: Int): M = shiftedBy(Pair(horizontally, vertically))
	/**
	  * @param amount Amount to shift this view horizontally
	  * @return A shifted copy of this view
	  */
	def shiftedHorizontallyBy(amount: Int) = shiftedBy(Pair(amount, 0))
	/**
	  * @param amount Amount to shift this view vertically
	  * @return A shifted copy of this view
	  */
	def shiftedVerticallyBy(amount: Int) = shiftedBy(Pair(0, amount))
}

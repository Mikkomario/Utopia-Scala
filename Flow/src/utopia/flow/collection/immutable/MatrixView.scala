package utopia.flow.collection.immutable

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.template
import utopia.flow.collection.template.MatrixViewLike

object MatrixView
{
	// OTHER    ----------------------------
	
	/**
	  * Creates a view into a matrix
	  * @param matrix A matrix
	  * @param viewArea Targeted view area. The actual view area will be limited to the matrix's area
	  * @tparam A Type of matrix cell values
	  * @return A view into the targeted matrix
	  */
	def apply[A](matrix: template.MatrixLike[A, _], viewArea: Pair[NumericSpan[Int]]): Matrix[A] = {
		viewArea.findMergeWith(matrix.size) { (span, len) =>
			if (len == 0) None else span.overlapWith(NumericSpan(0, len - 1))
		} match {
			case Some(area) => new _MatrixView[A](matrix, area)
			case None => Matrix.empty
		}
	}
	
	
	// NESTED   ----------------------------
	
	private class _MatrixView[+A](override protected val matrix: template.MatrixLike[A, _],
	                              override val viewArea: Pair[NumericSpan[Int]])
		extends MatrixView[A]
	{
		override lazy val columns = columnsView.map { _.toIndexedSeq }.toIndexedSeq
		override lazy val rows = rowsView.map { _.toIndexedSeq }.toIndexedSeq
	}
}

/**
  * A view into an immutable matrix
  * @author Mikko Hilpinen
  * @since 23.1.2023, v2.0
  */
trait MatrixView[+A] extends Matrix[A] with MatrixViewLike[A, Matrix[A]]
{
	override protected def withViewArea(area: Pair[NumericSpan[Int]]): MatrixView[A] =
		new MatrixView._MatrixView[A](matrix, area)
}

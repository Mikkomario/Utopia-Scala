package utopia.flow.collection.mutable

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.{Matrix, Pair}
import utopia.flow.collection.template.MatrixViewLike
import utopia.flow.operator.Identity
import utopia.flow.view.mutable.Pointer

object MutableMatrixView
{
	// OTHER    ----------------------------
	
	/**
	  * @param matrix Matrix to view
	  * @param viewArea Targeted view area.
	  *                 The actual view area will be cropped to the targeted matrix's size.
	  * @tparam A Type of matrix cell values
	  * @return A mutable view into the specified matrix
	  */
	def apply[A](matrix: MutableMatrix[A], viewArea: Pair[NumericSpan[Int]]) =
		viewArea.findMergeWith(matrix.size) { (span, len) =>
			if (len == 0) None else span.overlapWith(NumericSpan(0, len - 1))
		} match {
			case Some(area) => new _MutableMatrixView[A](matrix, area)
			case None => MutableMatrix.empty[A]()
		}
	
	
	// NESTED   ----------------------------
	
	private class _MutableMatrixView[A](override protected val matrix: MutableMatrix[A],
	                                    override val viewArea: Pair[NumericSpan[Int]])
		extends MutableMatrixView[A]
	{
		override lazy val pointers: Matrix[Pointer[A]] = matrix.pointers.view(viewArea)
	}
}

/**
  * A view into a mutable matrix
  * @author Mikko Hilpinen
  * @since 23.1.2023, v2.0
  */
trait MutableMatrixView[A] extends MutableMatrix[A] with MatrixViewLike[A, A, MutableMatrix[A]]
{
	// ABSTRACT -------------------------
	
	override protected def matrix: MutableMatrix[A]
	
	
	// IMPLEMENTED  ---------------------
	
	override def columnsView = originalColumnsView
	override def rowsView = originalRowsView
	
	override protected def viewFunction: Either[A => A, (A, Pair[Int]) => A] = Left(Identity)
	
	override protected def withViewArea(area: Pair[NumericSpan[Int]]): MutableMatrix[A] =
		new MutableMatrixView._MutableMatrixView[A](matrix, area)
}

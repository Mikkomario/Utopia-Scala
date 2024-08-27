package utopia.flow.collection.mutable

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.immutable.{Matrix, MatrixView, Pair}
import utopia.flow.collection.mutable.MutableMatrix.{EmptyMutableMatrix, MatrixReadView}
import utopia.flow.collection.template.MatrixLike
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.Pointer

object MutableMatrix
{
	// OTHER    -------------------------
	
	private lazy val emptyReadView = new MatrixReadView(Matrix.empty)
	
	/**
	  * @tparam A Type of cell values
	  * @return A new empty matrix
	  */
	def empty[A](): MutableMatrix[A] = new EmptyMutableMatrix[A]()
	/**
	  * Creates a matrix filled with empty values (i.e. None)
	  * @param size Size of the resulting matrix
	  * @tparam A Type of values that may be assigned to cells
	  * @return A new matrix with all values set as None
	  */
	def empty[A](size: Pair[Int]) = apply(Matrix.fill(size) { _ => Pointer.empty[A] })
	
	/**
	  * @param pointers A matrix with mutable pointer values
	  * @tparam A Type of cell values
	  * @return A new mutable matrix wrapping the specified pointer matrix
	  */
	def apply[A](pointers: Matrix[Pointer[A]]): MutableMatrix[A] = new _MutableMatrix[A](pointers)
	
	/**
	  * Creates a new matrix
	  * @param size Size of the matrix (width, height)
	  * @param f A function that accepts a cell coordinate and returns (initial) values to assign to each coordinate
	  * @tparam A Type of values assigned
	  * @return A new matrix
	  */
	def fill[A](size: Pair[Int])(f: Pair[Int] => A) =
		apply(Matrix.fill(size) { pos => Pointer(f(pos)) })
	/**
	  * Lazily initializes a new matrix
	  * @param size Size of the matrix (width, height)
	  * @param f    A function that accepts a cell coordinate and returns (initial) values to assign to each coordinate
	  * @tparam A Type of values assigned
	  * @return A new matrix
	  */
	def lazyFill[A](size: Pair[Int])(f: Pair[Int] => A) =
		apply(Matrix.lazyFill(size) { pos => Pointer(f(pos)) })
	
	
	// NESTED   -------------------------
	
	private class EmptyMutableMatrix[A] extends MutableMatrix[A]
	{
		override def pointers: Matrix[Pointer[A]] = Matrix.empty
		
		override protected def empty = this
		
		override def view(area: Pair[NumericSpan[Int]]) = this
	}
	
	private class _MutableMatrix[A](override val pointers: Matrix[Pointer[A]]) extends MutableMatrix[A]
	
	/**
	  * Used for providing a read-only access to a set of pointers
	  * @param pointers Pointers to view
	  * @tparam A Type of cell values in this matrix
	  */
	class MatrixReadView[+A](pointers: Matrix[View[A]]) extends MatrixLike[A, MatrixReadView[A]]
	{
		override def width = pointers.width
		override def height = pointers.height
		
		override def columnsView = pointers.columnsView.map { _.map { _.value } }
		override def rowsView = pointers.rowsView.map { _.map { _.value } }
		
		override protected def empty = emptyReadView
		
		override def apply(column: Int, row: Int) = pointers(column, row).value
		
		override def view(area: Pair[NumericSpan[Int]]) = new MatrixReadView(pointers.view(area))
	}
}

/**
  * Common trait for mutable matrix implementations
  * @author Mikko Hilpinen
  * @since 23.1.2023, v2.0
  */
trait MutableMatrix[A] extends MatrixLike[A, MutableMatrix[A]]
{
	// ABSTRACT ----------------------------
	
	/**
	  * @return A matrix containing the pointers used by this matrix
	  */
	def pointers: Matrix[Pointer[A]]
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return The current state of this matrix (immutable)
	  */
	def currentState = pointers.map { _.value }
	/**
	  * @return A read-only view into this matrix
	  */
	def readOnly = new MatrixReadView[A](pointers)
	
	
	// IMPLEMENTED  ------------------------
	
	override def width = pointers.width
	override def height = pointers.height
	
	override def columnsView = pointers.columns.view.map { _.view.map { _.value } }
	override def rowsView = pointers.rows.view.map { _.view.map { _.value } }
	
	override protected def empty: MutableMatrix[A] = new EmptyMutableMatrix[A]()
	
	override def apply(column: Int, row: Int) = pointers(column, row).value
	
	override def view(area: Pair[NumericSpan[Int]]): MutableMatrix[A] = MutableMatrixView(this, area)
	
	
	// OTHER    ---------------------------
	
	/**
	  * Assigns a new value to a single cell.
	  * Please note that this function does nothing when targeting a cell outside of this matrix.
	  * @param coordinates Targeted coordinates (x, y)
	  * @param value Value to assign (call-by-name)
	  */
	def update(coordinates: Pair[Int], value: => A) = pointers.lift(coordinates).foreach { _.value = value }
	/**
	  * Assigns a new value to a single cell.
	  * Please note that this function does nothing when targeting a cell outside of this matrix.
	  * @param column The targeted x-coordinate
	  * @param row The targeted y-coordinate
	  * @param value       Value to assign (call-by-name)
	  */
	def update(column: Int, row: Int, value: => A): Unit = update(Pair(column, row), value)
	/**
	  * Modifies the value of a single cell in this matrix.
	  * Please note that this function does nothing when targeting a cell outside of this matrix.
	  * @param coordinates Targeted coordinates (x, y)
	  * @param f A function for altering the cell value.
	  *          Accepts the current cell value and returns the new value to assign.
	  */
	def update(coordinates: Pair[Int])(f: A => A) = pointers.lift(coordinates).foreach { _.update(f) }
	
	/**
	  * Assigns a value to all cells within this matrix
	  * @param value The value to assign to all cells
	  */
	def setAll(value: A) = pointers.iterator.foreach { _.value = value }
	/**
	  * Updates the value of all cells within this matrix by utilizing the specified mapping function
	  * @param f A function for altering cell values. Accepts the current value, returns the new value to assign.
	  */
	def updateAll(f: A => A) = pointers.iterator.foreach { _.update(f) }
	/**
	  * Updates the value of all cells within this matrix by utilizing the specified mapping function
	  * @param f A function for altering cell values.
	  *          Accepts the current cell value, as well as the cell coordinates, returns the new value to assign.
	  */
	def updateAllWithIndex(f: (A, Pair[Int]) => A) =
		pointers.iteratorWithIndex.foreach { case (p, pos) => p.update { v => f(v, pos) } }
	
	/**
	  * Creates a read-only view to a specific area within this matrix
	  * @param area Targeted sub-region within this matrix.
	  *             First the horizontal area (columns) and then the vertical area (rows).
	  * @return A read-only view into the specified area within this matrix.
	  */
	def readView(area: Pair[NumericSpan[Int]]) = MatrixView(this, area)
}

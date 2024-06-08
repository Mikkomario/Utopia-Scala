package utopia.flow.collection.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Matrix.{SingleSequenceMatrix, _Matrix}
import utopia.flow.collection.immutable.caching.iterable.LazyVector
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.enumeration.End.{First, Last}
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

import scala.collection.{AbstractIndexedSeqView, IndexedSeqView}

object Matrix
{
	// ATTRIBUTES   --------------------------
	
	/**
	 * An empty matrix
	 */
	lazy val empty: Matrix[Nothing] = new EmptyMatrix()
	
	
	// OTHER    ------------------------------
	
	/**
	  * Creates a new matrix from a single sequence of values
	  * @param values Values that form this matrix
	  * @param width The width of the matrix (call-by-name / lazily called)
	  * @param height The length of the matrix (call-by-name / lazily called)
	  * @param rowsToColumns Whether the specified values are listed primarily from left to right (rows), and
	  *                      secondarily from top to bottom (columns).
	  *                      If false (default), expects the values to be listed from top to bottom first,
	  *                      and then from left to right.
	  * @tparam A Type of values in this matrix
	  * @return A new matrix that contains the specified values
	  */
	def apply[A](values: IndexedSeq[A], width: => Int, height: => Int, rowsToColumns: Boolean = false) = {
		if (values.isEmpty)
			empty
		else
			new SingleSequenceMatrix[A](values, Pair(Lazy(width), Lazy(height)), isRows = rowsToColumns)
	}
	/**
	 * @param cols A set of columns, from left to right.
	 *             NB: Each column must have the same length.
	 * @tparam A Type of cell values
	 * @return A new matrix consisting of those columns
	 */
	def withColumns[A](cols: IndexedSeq[IndexedSeq[A]]): Matrix[A] = cols.headOption match {
		case Some(firstCol) => new _Matrix[A](cols, Pair(Lazy { cols.size }, Lazy { firstCol.size }))
		case None => empty
	}
	/**
	 * @param rows A set of rows, from top to bottom.
	 *             NB: Each column must have the same length.
	 * @tparam A Type of cell values
	 * @return A new matrix consisting of those rows
	 */
	def withRows[A](rows: IndexedSeq[IndexedSeq[A]]): Matrix[A] = rows.headOption match {
		case Some(firstRow) => new _Matrix[A](rows, Pair(Lazy { firstRow.size }, Lazy { rows.size }), isRows = true)
		case None => empty
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
	
	/**
	  * Creates a transposed matrix (view). Transposing means switching the rows to columns and columns to rows.
	  * @param matrix A matrix to transpose
	  * @tparam A Type of items within the specified matrix
	  * @return A transposed view into the specified matrix.
	  */
	def transpose[A](matrix: Matrix[A]): Matrix[A] = new TransposedMatrixView[A](matrix)
	
	
	// NESTED   ------------------------------
	
	private class EmptyMatrix[+A] extends Matrix[A]
	{
		override lazy val columns: IndexedSeq[IndexedSeq[A]] = Empty
		override lazy val rows: IndexedSeq[IndexedSeq[A]] = Empty
		override lazy val columnsView: IndexedSeqView[IndexedSeqView[A]] = new EmptyIndexedSeqView[IndexedSeqView[A]]()
		override lazy val rowsView: IndexedSeqView[IndexedSeqView[A]] = new EmptyIndexedSeqView[IndexedSeqView[A]]()
		
		override def width: Int = 0
		override def height: Int = 0
		override protected def sizeView: Pair[View[Int]] = Pair.twice(View.fixed(0))
		
		override def transpose: Matrix[A] = this
		
		override def view(area: Pair[NumericSpan[Int]]) = this
		
		override def mapColumns[B](f: IndexedSeqView[A] => IterableOnce[B]): Matrix[B] = Matrix.empty
		override def mapRows[B](f: IndexedSeqView[A] => IterableOnce[B]): Matrix[B] = Matrix.empty
		override def mapColumnsWithIndex[B](f: (IndexedSeqView[A], Int) => IterableOnce[B]): Matrix[B] = Matrix.empty
		override def mapRowsWithIndex[B](f: (IndexedSeqView[A], Int) => IterableOnce[B]): Matrix[B] = Matrix.empty
		override def map[B](f: A => B): Matrix[B] = Matrix.empty
		override def mapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = Matrix.empty
		override def lazyMap[B](f: A => B): Matrix[B] = Matrix.empty
		override def lazyMapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = Matrix.empty
	}
	
	// Places the Matrix contents in a single sequence (view)
	private class SingleSequenceMatrix[A](values: IndexedSeq[A], override val sizeView: Pair[View[Int]],
	                                      isRows: Boolean = false)
		extends Matrix[A]
	{
		// ATTRIBUTES   --------------------------
		
		override lazy val columnsView = {
			if (isRows)
				new TransposedSequencesView[A](values.view, height, width)
			else
				new SequencesView[A](values.view, height, width)
		}
		override lazy val rowsView = {
			if (isRows)
				new SequencesView[A](values.view, width, height)
			else
				new TransposedSequencesView[A](values.view, width, height)
		}
		
		override lazy val columns: IndexedSeq[IndexedSeq[A]] = columnsView.map { _.toIndexedSeq }.toIndexedSeq
		override lazy val rows: IndexedSeq[IndexedSeq[A]] = rowsView.map { _.toIndexedSeq }.toIndexedSeq
		
		
		// IMPLEMENTED  ------------------------
		
		override def width: Int = sizeView.first.value
		override def height: Int = sizeView.second.value
		
		override def transpose: Matrix[A] = new TransposedMatrixView[A](this)
		
		override def iterator = values.iterator
		override def iteratorByColumns = if (isRows) super.iteratorByColumns else values.iterator
		override def iteratorByRows = if (isRows) values.iterator else super.iteratorByRows
		
		override def apply(column: Int, row: Int) = {
			if (isRows)
				values(column + row * width)
			else
				values(row + column * height)
		}
		
		override def map[B](f: A => B): Matrix[B] = new SingleSequenceMatrix[B](values.map(f), sizeView, isRows)
		override def mapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = mapToConcreteWithIndex(f)
		
		override def lazyMap[B](f: A => B): Matrix[B] = new SingleSequenceMatrix[B](values.lazyMap(f), sizeView, isRows)
		override def lazyMapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = lazyMapToConcreteWithIndex(f)
	}
	
	private class _Matrix[A](data: IndexedSeq[IndexedSeq[A]], override val sizeView: Pair[View[Int]],
	                         isRows: Boolean = false)
		extends Matrix[A]
	{
		// ATTRIBUTES   -----------------------
		
		override lazy val columnsView: IndexedSeqView[IndexedSeqView[A]] = {
			if (isRows)
				(0 until width).view.map { x => rowsView.map { _(x) } }
			else
				data.view.map { _.view }
		}
		override lazy val rowsView: IndexedSeqView[IndexedSeqView[A]] = {
			if (isRows)
				data.view.map { _.view }
			else
				(0 until height).view.map { y => columnsView.map { _(y) } }
		}
		
		override lazy val columns: IndexedSeq[IndexedSeq[A]] =
			if (isRows) columnsView.map { _.toIndexedSeq }.toIndexedSeq else data
		override lazy val rows: IndexedSeq[IndexedSeq[A]] =
			if (isRows) data else rowsView.map { _.toIndexedSeq }.toIndexedSeq
		
		
		// IMPLEMENTED  ---------------------
		
		override def width: Int = sizeView.first.value
		override def height: Int = sizeView.second.value
		
		override def transpose: Matrix[A] = new TransposedMatrixView[A](this)
		
		override def iterator = data.iterator.flatten
		override def iteratorByColumns = if (isRows) super.iteratorByColumns else data.iterator.flatten
		override def iteratorByRows = if (isRows) data.iterator.flatten else super.iteratorByRows
		
		override def map[B](f: A => B): Matrix[B] = new _Matrix[B](data.map { _.map(f) }, sizeView, isRows)
		override def mapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = mapToConcreteWithIndex(f)
		
		override def lazyMap[B](f: A => B): Matrix[B] = new _Matrix[B](data.lazyMap { _.lazyMap(f) }, sizeView)
		override def lazyMapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = lazyMapToConcreteWithIndex(f)
	}
	
	private class TransposedMatrixView[A](target: Matrix[A]) extends Matrix[A]
	{
		override def columnsView: IndexedSeqView[IndexedSeqView[A]] = target.rowsView
		override def rowsView: IndexedSeqView[IndexedSeqView[A]] = target.columnsView
		
		override def columns: IndexedSeq[IndexedSeq[A]] = target.rows
		override def rows: IndexedSeq[IndexedSeq[A]] = target.columns
		
		override def transpose: Matrix[A] = target
		
		override def width: Int = target.height
		override def height: Int = target.width
		override protected def sizeView: Pair[View[Int]] = target.sizeView
		
		override def iterator = target.iterator
		override def iteratorByColumns = target.iteratorByRows
		override def iteratorByRows = target.iteratorByColumns
		
		override def mapColumns[B](f: IndexedSeqView[A] => IterableOnce[B]): Matrix[B] = target.mapRows(f).transpose
		override def mapRows[B](f: IndexedSeqView[A] => IterableOnce[B]): Matrix[B] = target.mapColumns(f).transpose
		override def mapColumnsWithIndex[B](f: (IndexedSeqView[A], Int) => IterableOnce[B]): Matrix[B] =
			target.mapRowsWithIndex(f).transpose
		override def mapRowsWithIndex[B](f: (IndexedSeqView[A], Int) => IterableOnce[B]): Matrix[B] =
			target.mapColumnsWithIndex(f).transpose
		
		override def map[B](f: A => B): Matrix[B] = target.map(f).transpose
		override def mapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = mapToConcreteWithIndex(f)
		
		override def lazyMap[B](f: A => B): Matrix[B] = target.lazyMap(f).transpose
		override def lazyMapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = lazyMapToConcreteWithIndex(f)
	}
	
	private class SequencesView[A](values: IndexedSeqView[A], lengthOfValue: Int, override val length: Int)
		extends AbstractIndexedSeqView[IndexedSeqView[A]]
	{
		override def apply(i: Int): IndexedSeqView[A] = {
			val startIndex = i * lengthOfValue
			values.slice(startIndex, startIndex + lengthOfValue)
		}
	}
	private class TransposedSequencesView[+A](values: IndexedSeqView[A], lengthOfValue: Int, override val length: Int)
		extends AbstractIndexedSeqView[IndexedSeqView[A]]
	{
		override def apply(i: Int) = new TransposedSeqView[A](values, i, length, lengthOfValue)
	}
	private class TransposedSeqView[+A](values: IndexedSeqView[A], startIndex: Int, increment: Int,
	                                   override val length: Int)
		extends AbstractIndexedSeqView[A]
	{
		override def apply(i: Int): A = values(startIndex + increment * i)
	}
}

/**
 * A 2-dimensional grid data set
 * @author Mikko Hilpinen
 * @since 22.1.2023, v2.0
 * @tparam A Type of cell values in this matrix
 */
trait Matrix[+A] extends MatrixLike[A, Matrix, Matrix[A]]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Views to the width and height of this matrix
	  */
	protected def sizeView: Pair[View[Int]]
	
	
	// IMPLEMENTED  -----------------------
	
	override def self: Matrix[A] = this
	override protected def empty: Matrix[A] = Matrix.empty
	
	/**
	  * @param area Viewed area, as an x-range (for columns) and then an y-range (for rows)
	  * @return A view of this matrix that covers only the specified sub-region.
	  *         Non-overlapping part (i.e. the targeted area outside of this matrix) of the area is not included.
	  */
	def view(area: Pair[NumericSpan[Int]]) = MatrixView(this, area)
	
	override def mapColumns[B](f: IndexedSeqView[A] => IterableOnce[B]): Matrix[B] = mapColumnsOrRows()(f)
	override def mapRows[B](f: IndexedSeqView[A] => IterableOnce[B]): Matrix[B] = mapColumnsOrRows(makeRows = true)(f)
	
	override def mapColumnsWithIndex[B](f: (IndexedSeqView[A], Int) => IterableOnce[B]): Matrix[B] =
		mapColumnsOrRowsWithIndex()(f)
	override def mapRowsWithIndex[B](f: (IndexedSeqView[A], Int) => IterableOnce[B]): Matrix[B] =
		mapColumnsOrRowsWithIndex(makeRows = true)(f)
	
	
	// OTHER    --------------------------
	
	/**
	  * Maps this matrix to a concrete (i.e. fully cached) matrix
	  * @param f Mapping function to use. Accepts both the item in this matrix, plus the index at which that item appears
	  * @tparam B Type of mapping results
	  * @return New matrix that consists of mapped values
	  */
	protected def mapToConcreteWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = {
		val iterator = iteratorWithIndex.map { case (item, index) => f(item, index) }
		if (iterator.hasNext)
			new SingleSequenceMatrix[B](iterator.toIndexedSeq, sizeView)
		else
			Matrix.empty
	}
	/**
	  * Maps this matrix to a concrete (i.e. fully cached) matrix.
	  * The mapping functions are called lazily, however.
	  * @param f        Mapping function to use. Accepts both the item in this matrix, plus the index at which that item appears
	  * @tparam B Type of mapping results
	  * @return New matrix that consists of lazily mapped values
	  */
	protected def lazyMapToConcreteWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = {
		val iterator = iteratorWithIndex.map { case (item, index) => Lazy { f(item, index) } }
		if (iterator.hasNext)
			new SingleSequenceMatrix[B](LazyVector(iterator), sizeView)
		else
			Matrix.empty
	}
	/**
	  * Replaces the columns or rows in this matrix. The result is fully cached.
	  * @param makeRows Whether rows (true) or columns (false) should be mapped / constructed
	  * @param f A mapping function that accepts a view to a row or a column in this matrix, and produces the
	  *          replacing row or column
	  * @tparam B Type of individual values in the mapping results
	  * @return Mapped copy of this matrix
	  */
	protected def mapColumnsOrRows[B](makeRows: Boolean = false)
	                                 (f: IndexedSeqView[A] => IterableOnce[B]) =
		replaceColumnsOrRows(makeRows) {
			val coll = if (makeRows) rowsView else columnsView
			coll.map { c => IndexedSeq.from(f(c)) }.toIndexedSeq
		}
	/**
	  * Replaces the columns or rows in this matrix. The result is fully cached.
	  * @param makeRows Whether rows (true) or columns (false) should be mapped / constructed
	  * @param f        A mapping function that accepts a view to a row or a column in this matrix, and produces the
	  *                 replacing row or column.
	  *                 Also accepts the index at which that column or row appears (0-based).
	  * @tparam B Type of individual values in the mapping results
	  * @return Mapped copy of this matrix
	  */
	protected def mapColumnsOrRowsWithIndex[B](makeRows: Boolean = false)
	                                        (f: (IndexedSeqView[A], Int) => IterableOnce[B]) =
		replaceColumnsOrRows(makeRows) {
			val coll = if (makeRows) rowsView else columnsView
			coll.zipWithIndex.map { case (c, i) => IndexedSeq.from(f(c, i)) }.toIndexedSeq
		}
	/**
	  * Replaces the columns or rows in this matrix. The result is fully cached.
	  * @param makeRows Whether rows (true) or columns (false) should be mapped / constructed
	  * @param newCollRow A function that produces new rows or columns to use
	  * @tparam B Type of individual values in the new rows or columns
	  * @return New matrix
	  */
	protected def replaceColumnsOrRows[B](makeRows: => Boolean = false)
	                                     (newCollRow: => IndexedSeq[IndexedSeq[B]]): Matrix[B] =
	{
		if (isEmpty)
			Matrix.empty
		else {
			val coll = newCollRow
			val newSizeView = sizeView.withSide(Lazy { columns.head.size }, if (makeRows) First else Last)
			new _Matrix[B](coll, newSizeView, isRows = makeRows)
		}
	}
}
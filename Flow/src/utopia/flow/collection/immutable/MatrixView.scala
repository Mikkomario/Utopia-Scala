package utopia.flow.collection.immutable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching.iterable.LazyVector
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.collection.template
import utopia.flow.collection.template.MatrixViewLike
import utopia.flow.operator.Identity
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

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
	
	private class _MatrixView[A](override protected val matrix: template.MatrixLike[A, _],
	                              override val viewArea: Pair[NumericSpan[Int]])
		extends MatrixView[A, A]
	{
		private lazy val views = Pair(originalColumnsView, originalRowsView)
		private lazy val cachedData = views.map { v => Lazy { v.map { _.toIndexedSeq }.toIndexedSeq } }
		
		override def columns = cachedData.first.value
		override def rows = cachedData.second.value
		
		override protected def viewFunction = Left(Identity)
		
		override def columnsView = views.first
		override def rowsView = views.second
		
		override def cached: Matrix[A] =
			_cachedFrom(cachedData.first.current, cachedData.second.current, Vector.from(iteratorByColumns))
		override def transpose: Matrix[A] = Matrix.transpose(this)
		
		override def map[B](f: A => B): Matrix[B] = new MappingMatrixView[B, A](matrix, viewArea, Left(f))
		override def mapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] =
			new MappingMatrixView[B, A](matrix, viewArea, Right(f))
		
		override def lazyMap[B](f: A => B): Matrix[B] =
			Matrix(LazyVector(iteratorByColumns.map { v => Lazy { f(v) } }), width, height)
		
		override protected def withViewArea(area: Pair[NumericSpan[Int]]): Matrix[A] = new _MatrixView[A](matrix, area)
	}
	private class MappingMatrixView[+A, O](override protected val matrix: template.MatrixLike[O, _],
	                                       override val viewArea: Pair[NumericSpan[Int]],
	                                       override val viewFunction: Either[O => A, (O, Pair[Int]) => A])
		extends MatrixView[A, O]
	{
		private lazy val views = Pair(super.columnsView, super.rowsView)
		private lazy val cachedData = views.map { v => Lazy { v.map { _.toIndexedSeq }.toIndexedSeq } }
		
		override def columnsView = views.first
		override def rowsView = views.second
		
		override def columns = cachedData.first.value
		override def rows = cachedData.second.value
		
		override def cached: Matrix[A] = _cachedFrom(cachedData.first.current, cachedData.second.current,
			viewFunction match {
				case Left(f) => LazyVector(indexIteratorByColumns.map { p => Lazy { f(original(p)) } })
				case Right(f) => LazyVector(indexIteratorByColumns.map { p => Lazy { f(original(p), p) } })
			})
		override def transpose: Matrix[A] = Matrix.transpose(this)
		
		override def map[B](f: A => B): Matrix[B] = {
			val newViewFunction = viewFunction
				.mapBoth { f1 => v: O => f(f1(v)) } { f1 => (v: O, p: Pair[Int]) => f(f1(v, p)) }
			new MappingMatrixView[B, O](matrix, viewArea, newViewFunction)
		}
		override def mapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = {
			val newViewFunction: (O, Pair[Int]) => B = viewFunction match {
				case Left(f1) => (v, p) => f(f1(v), p)
				case Right(f1) => (v, p) => f(f1(v, p), p)
			}
			new MappingMatrixView[B, O](matrix, viewArea, Right(newViewFunction))
		}
		
		override def lazyMap[B](f: A => B): Matrix[B] = {
			val valuesIterator = viewFunction match {
				case Left(f1) => indexIteratorByColumns.map { p => Lazy { f(f1(original(p))) } }
				case Right(f1) => indexIteratorByColumns.map { p => Lazy { f(f1(original(p), p)) } }
			}
			Matrix(LazyVector(valuesIterator), width, height)
		}
		override def lazyMapWithIndex[B](f: (A, Pair[Int]) => B) = {
			val valuesIterator = viewFunction match {
				case Left(f1) => indexIteratorByColumns.map { p => Lazy { f(f1(original(p)), p) } }
				case Right(f1) => indexIteratorByColumns.map { p => Lazy { f(f1(original(p), p), p) } }
			}
			Matrix(LazyVector(valuesIterator), width, height)
		}
		
		override protected def withViewArea(area: Pair[NumericSpan[Int]]): Matrix[A] =
			new MappingMatrixView(matrix, area, viewFunction)
	}
}

/**
  * A view into an immutable matrix
  * @author Mikko Hilpinen
  * @since 23.1.2023, v2.0
  */
trait MatrixView[+A, O] extends Matrix[A] with MatrixViewLike[A, O, Matrix[A]]
{
	// COMPUTED ---------------------------
	
	/**
	  * @return Copy of this view where the data is cached and extracted from the viewed matrix.
	  *         May require iteration and caching of the viewed area.
	  */
	def cached: Matrix[A]
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def sizeView: Pair[View[Int]] = Pair(View(width), View(height))
	
	override def lazyMapWithIndex[B](f: (A, Pair[Int]) => B): Matrix[B] = lazyMapToConcreteWithIndex(f)
	
	
	// OTHER    --------------------------
	
	/**
	  * Creates a cached view based on this view, optimizing based on which data has been cached
	  * @param cachedColumns Columns that have been cached. None if not cached.
	  * @param cachedRows Rows that have been cached. None if not cached.
	  * @param seq All values of this matrix as a single sequence (cols to rows order)
	  * @tparam B Type of resulting matrix values
	  * @return A cached matrix based on the presented data
	  */
	protected def _cachedFrom[B](cachedColumns: Option[IndexedSeq[IndexedSeq[B]]],
	                             cachedRows: => Option[IndexedSeq[IndexedSeq[B]]], seq: => IndexedSeq[B]) =
		cachedColumns match {
			case Some(cols) => Matrix.withColumns(cols)
			case None =>
				cachedRows match {
					case Some(rows) => Matrix.withRows(rows)
					case None => Matrix(seq, width, height)
				}
		}
}

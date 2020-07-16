package utopia.genesis.shape.template

import utopia.genesis.util.{ApproximatelyEquatable, Scalable}

/**
  * A common trait for matrix implementations
  * @author Mikko Hilpinen
  * @since 15.7.2020, v2.3
  */
trait MatrixLike[V <: VectorLike[V], +Repr] extends Dimensional[V] with Scalable[Repr]
	with ApproximatelyEquatable[Dimensional[V]]
{
	// ABSTRACT	---------------------
	
	// x-transformation at index 0, y-transformation at index 1 and so on
	def columns: Vector[V]
	
	/**
	  * @return The rows in this matrix
	  */
	def rows: Vector[V]
	
	/**
	  * The determinant of this matrix. The determinant shows the scaling applied to the volume
	  * (in case of 3x3) or area (in case of 2x2) that is caused by this transformation. Eg. If determinant is 3,
	  * that means that this transformation triples an area's volume.
	  * If determinant is negative, that means that one of the axes was flipped, meaning that the resulting shape will
	  * be mirrored along an axis. If determinant is 0, it means that this transformation reduces the shape to
	  * a lower dimensional space (2D plane, 1D line or 0D point).
	  */
	def determinant: Double
	
	/**
	  * An inverted copy of this matrix. The inverse matrix represents an inverse transformation of this transformation.
	  * Multiplying this matrix with the inverse matrix yields an identity matrix.
	  */
	def inverse: Option[Repr]
	
	protected def buildCopy(columns: Vector[V]): Repr
	
	
	
	// IMPLEMENTED	----------------
	
	override def dimensions = columns
	
	override def *(mod: Double) = map { _ * mod }
	
	override def ~==(other: Dimensional[V]) =
	{
		if (dimensions.size == other.dimensions.size)
			dimensions.zip(other.dimensions).forall { case (a, b) => a ~== b }
		else
			false
	}
	
	override def toString =
	{
		val content = toMap.map { case (axis, vector) =>
			s"$axis: [${vector.dimensions.mkString(",")}]" }.mkString(", ")
		s"{$content}"
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param columnIndex Index of the targeted column (the "x-coordinate" in this matrix)
	  * @param rowIndex The index of the targeted row (the "y-coordinate" in this matrix)
	  * @return Number at that location
	  * @throws IndexOutOfBoundsException If this matrix doesn't contain such indices
	  */
	@throws[IndexOutOfBoundsException]("This matrix doesn't contain specified index")
	def apply(columnIndex: Int, rowIndex: Int) = column(columnIndex).dimensions(rowIndex)
	
	def column(index: Int) = columns(index)
	
	def row(index: Int) = rows(index)
	
	// Vector multiplication: x*x-transformation + y*y-transformation
	// TODO: Handle the case of 0-dimension vectors
	def *(vector: V): V = vector.dimensions.zip(columns)
		.map { case (c, transformation) => transformation * c }
		.reduce { _ + _ }
	
	def *(matrix: MatrixLike[V, _]): Repr = buildCopy(matrix.columns.map { this * _ })
	
	/**
	  * Transforms the specified vector. Same as using vector multiplication (*)
	  * @param vector Vector to transform
	  * @return A transformed vector
	  */
	def apply(vector: V) = this * vector
	
	/**
	  * Transforms the specified matrix. Same as using matrix multiplication (*)
	  * @param matrix Matrix to transform
	  * @return A transformed matrix
	  */
	def apply(matrix: MatrixLike[V, _]) = this * matrix
	
	// Determinant = how much the area is scaled, proportionally (Eg. 2x scaling both x and y yields determinant 4 =(2^2))
	// NB: When determinant is 0, there is only a single line or a single point, no 2d space
	// NBB: When determinant is negative, space is flipped (like with scaling in general)
	// In 3D, we're dealing with volumes
	
	/**
	  * Performs the specified mapping function on each of the numbers in this matrix
	  * @param f A mapping function
	  * @return A mapped copy of this matrix
	  */
	def map(f: Double => Double) = buildCopy(columns.map { _.map(f) })
	
	/**
	  * Maps all items in this matrix
	  * @param f A mapping function that, in addition to the number, takes the
	  *          <b>index of column (first) and index of row (second)</b>
	  *          where the number resides. Both coordinates start from 0. On 2D plane these parameters would be (x, y)
	  * @return A mapped copy of this matrix
	  */
	def mapWithIndices(f: (Double, Int, Int) => Double) =
		buildCopy(columns.zipWithIndex.map { case (column, columnIndex) =>
			column.mapWithIndex { (v, rowIndex) => f(v, columnIndex, rowIndex) } })
}

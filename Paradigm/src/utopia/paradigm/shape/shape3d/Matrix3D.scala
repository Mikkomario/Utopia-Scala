package utopia.paradigm.shape.shape3d

import utopia.flow.util.CollectionExtensions._
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.shape2d.{Matrix2D, Vector2D, Vector2DLike}
import utopia.paradigm.shape.template.MatrixLike
import utopia.paradigm.transform.{AffineTransformable, JavaAffineTransformConvertible, LinearTransformable}

import java.awt.geom.AffineTransform

object Matrix3D
{
	// ATTRIBUTES	------------------------------
	
	/**
	  * A transformation that keeps vectors as they are (I * V = V)
	  */
	val identity = apply(
		1, 0, 0,
		0, 1, 0,
		0, 0, 1
	)
	
	
	// OTHER	----------------------------------
	
	/**
	  * Creates a matrix by listing the numbers from left to right, top to bottom
	  * @param xx X-component of the x-transformation
	  * @param yx X-component of the y-transformation
	  * @param zx X-component of the z-transformation
	  * @param xy Y-component of the x-transformation
	  * @param yy Y-component of the y-transformation
	  * @param zy Y-component of the z-transformation
	  * @param xz Z-component of the x-transformation
	  * @param yz Z-component of the y-transformation
	  * @param zz Z-component of the z-transformation
	  * @return A new 3x3 matrix with specified values
	  */
	def apply(xx: Double, yx: Double, zx: Double, xy: Double, yy: Double, zy: Double,
			  xz: Double, yz: Double, zz: Double): Matrix3D =
		Matrix3D(Vector3D(xx, xy, xz), Vector3D(yx, yy, yz), Vector3D(zx, zy, zz))
	
	/**
	  * @param columns A set of columns
	  * @return A matrix with the specified columns (3 expected, if less, zero vectors are used)
	  */
	def withColumns(columns: Seq[Vector3D]) =
	{
		val c = columns.padTo(3, Vector3D.zero)
		Matrix3D(c.head, c(1), c(2))
	}
	
	/**
	  * @param rows A set of rows
	  * @return A matrix with specified rows (3 expected, if less, zero vectors are used)
	  */
	def withRows(rows: Seq[Vector3D]) =
	{
		val r = rows.padTo(3, Vector3D.zero)
		val columns = (0 until 3).map { i => r.map { row => row.dimensions(i) } }.map(Vector3D.withDimensions)
		Matrix3D(columns.head, columns(1), columns(2))
	}
	
	/**
	  * Creates an affine transformation by combining a linear transformation and a translation transformation
	  * @param linear Linear transformation to apply
	  * @param translation Translation to apply
	  * @return A new affine transformation that applies both the linear transformation and the translation
	  */
	// See: https://en.wikipedia.org/wiki/Transformation_matrix
	def affineTransform(linear: Matrix2D, translation: Vector2D) = apply(linear.xTransform.in3D,
		linear.yTransform.in3D, translation.withZ(1))
	
	/**
	  * Creates a translating affine transformation
	  * @param amount Amount of translation to apply
	  * @return A new affine transformation that only translates the shape
	  */
	def translation(amount: Vector2DLike[_]) = apply(
		1, 0, amount.x,
		0, 1, amount.y,
		0, 0, 1)
}

/**
  * Represents a 3x3 matrix, which can also be visualized as a 3-dimensional transformation function.
  * @author Mikko Hilpinen
  * @since Genesis 15.7.2020, v2.3
  */
case class Matrix3D(xTransform: Vector3D = Vector3D.zero, yTransform: Vector3D = Vector3D.zero,
					zTransform: Vector3D = Vector3D.zero)
	extends MatrixLike[Vector3D, Matrix3D] with ThreeDimensional[Vector3D] with AffineTransformable[Matrix3D]
		with JavaAffineTransformConvertible with LinearTransformable[Matrix3D]
{
	// ATTRIBUTES   ------------------------------
	
	/**
	  * The determinant of this matrix. The determinant shows the scaling applied to the volume that is caused by this
	  * transformation. Eg. If determinant is 3, that means that this transformation triples an area's volume.
	  * If determinant is negative, that means that one of the axes was flipped, meaning that the resulting shape will
	  * be mirrored along an axis. If determinant is 0, it means that this transformation reduces the shape to
	  * a 2D plane, 1D line or 0D point.
	  */
	override lazy val determinant =
	{
		// Starts by finding the row with most zeros
		val referenceRowIndex = rows.indices.maxBy { i => row(i).toVector.count { _ == 0.0 } }
		val referenceRow = row(referenceRowIndex)
		// Uses that row to calculate the determinant
		val components = columns.indices.map { columnIndex =>
			val multiplier = referenceRow.toVector(columnIndex)
			// Skips calculations when result would be multiplied by a zero
			if (multiplier == 0.0)
				multiplier
			else
			{
				// Creates a 2x2 matrix that doesn't include numbers in same column or row
				val remainingMatrix = dropTo2D(columnIndex, referenceRowIndex)
				// The determinant of that matrix determines the final result
				remainingMatrix.determinant * multiplier
			}
		}
		
		// Adds the components together using x - y + z, if columns were swapped (so that sign is affected), inverts the result
		val combination = components.head - components(1) + components(2)
		if (referenceRowIndex == 1)
			-combination
		else
			combination
	}
	
	/**
	  * An inverted copy of this matrix. The inverse matrix represents an inverse transformation of this transformation.
	  * Multiplying this matrix with the inverse matrix yields an identity matrix.
	  */
	override lazy val inverse =
	{
		// Inverse matrix is this matrix's adjugate divided by this matrix's determinant
		if (determinant == 0.0)
			None
		else
			Some(adjugate / determinant)
	}
	
	override val columns = Vector(xTransform, yTransform, zTransform)
	
	override lazy val rows = Vector(
		Vector3D(xTransform.x, yTransform.x, zTransform.x),
		Vector3D(xTransform.y, yTransform.y, zTransform.y),
		Vector3D(xTransform.z, yTransform.z, zTransform.z)
	)
	
	
	// COMPUTED	----------------------------------
	
	/**
	  * @return A transposed copy of this matrix (A matrix with rows of this matrix as columns)
	  */
	def transposed = Matrix3D.withRows(columns)
	
	/**
	  * @return A copy of this matrix that has first been transposed, with then each value replaced by the determinant
	  *         of their corresponding minor 2x2 matrix, with cofactors.
	  */
	def adjugate =
	{
		val t = transposed
		t.mapWithIndices { (_, colId, rowId) =>
			val minorDeterminant = t.dropTo2D(colId, rowId).determinant
			// Also adds a sign based on matrix
			// + - +
			// - + -
			// + - + (Every second item is negated)
			minorDeterminant * (if ((rowId * 3 + colId) % 2 == 0) 1 else -1)
		}
	}
	
	/**
	  * @return A copy of this matrix that has been reduced to two dimensions. Will lose the z-component of each
	  *         transformation and the z-transformation entirely.
	  */
	def in2D = Matrix2D(xTransform.in2D, yTransform.in2D)
	
	/**
	  * @return The linear transform portion and the translation portion of this matrix when it is considered an
	  *         affine transformation
	  */
	def linearAndTranslation = in2D -> zTransform.in2D
	
	
	// IMPLEMENTED	------------------------------
	
	override def repr = this
	
	override protected def buildCopy(columns: Seq[Vector3D]) =
	{
		val fullColumns = columns.padTo(3, Vector3D.zero)
		Matrix3D(fullColumns.head, fullColumns(1), fullColumns(2))
	}
	
	override protected def zeroDimension = Vector3D.zero
	
	override def transformedWith(transformation: Matrix3D) = transformation(this)
	
	override def transformedWith(transformation: Matrix2D) = transformation.to3D(this)
	
	/**
	  * @return A java geom AffineTransform based on this matrix. Assumes the first two z-transformation arguments to
	  *         be x and y translations and the top left 2x2 matrix to be the linear transformation part.
	  */
	// This matrix uses coordinates (column (x), row (y))
	// Affine transform uses coordinates (row (y), column (x))
	override def toJavaAffineTransform = new AffineTransform(
		apply(0,0), apply(0,1),
		apply(1,0), apply(1,1), apply(2,0), apply(2,1))
	
	
	// OTHER	-------------------------------
	
	/**
	  * Takes a 2x2 area of this matrix as a separate matrix
	  * @param columnIndexToDrop Index of the column to exclude from the resulting matrix [0, 2]
	  * @param rowIndexToDrop Index of the row to exclude from the resulting matrix [0, 2]
	  * @return A 2x2 matrix containing the remaining items
	  */
	def dropTo2D(columnIndexToDrop: Int, rowIndexToDrop: Int) =
	{
		val newColumns = columns.withoutIndex(columnIndexToDrop).map { _.withoutDimensionAtIndex(rowIndexToDrop) }
		Matrix2D(newColumns.head, newColumns(1))
	}
	
	/**
	  * Takes a 2x2 area of this matrix as a separate matrix
	  * @param transformationToDrop Type of transformation to exclude from the resulting matrix
	  *                             Eg. if dropping Y, resulting matrix will contain X and Z transformations.
	  * @param dimensionToDrop Dimension to drop from all the included transformations. Eg. if dropping Z,
	  *                        matrix vectors will only contain their X and Y components
	  * @return A 2x2 matrix containing the remaining items
	  */
	def dropTo2D(transformationToDrop: Axis, dimensionToDrop: Axis): Matrix2D =
		dropTo2D(indexForAxis(transformationToDrop), indexForAxis(dimensionToDrop))
	
	/**
	  * @param transformable An instance to transform
	  * @tparam A Type of transformation result
	  * @return Transformed instance
	  */
	def transform[A](transformable: AffineTransformable[A]) = transformable.transformedWith(this)
}

package utopia.genesis.shape.shape2D

import utopia.genesis.shape.shape3D.Matrix3D
import utopia.genesis.shape.template.MatrixLike

object Matrix2D
{
	// ATTRIBUTES	---------------------------
	
	/**
	  * A 2D transformation that keeps the vectors as is (I * V = V)
	  */
	val identity = apply(
		1, 0,
		0, 1
	)
	
	
	// OTHER	-------------------------------
	
	/**
	  * Creates a new matrix by providing the numbers from left to right, up to down
	  * @param xx X-component of the x-transformation
	  * @param yx X-component of the y-transformation
	  * @param xy Y-component of the x-transformation
	  * @param yy Y-component of the y-transformation
	  * @return A matrix that consists of the two transformations
	  */
	def apply(xx: Double, yx: Double, xy: Double, yy: Double): Matrix2D = Matrix2D(Vector2D(xx, xy), Vector2D(yx, yy))
}

/**
  * Represents a 2x2 matrix, which can be visualized as a 2-dimensional transformation
  * @author Mikko Hilpinen
  * @since 15.7.2020, v2.3
  */
case class Matrix2D(xTransform: Vector2D = Vector2D.zero, yTransform: Vector2D = Vector2D.zero)
	extends MatrixLike[Vector2D, Matrix2D] with TwoDimensional[Vector2D]
{
	// COMPUTED	--------------------------------
	
	// [[x1, y1], [x2, y2]] => det = x1*y2 - y1*x2, kind of a cross between this matrix
	/**
	  * @return The determinant of this matrix. Determinant can be interpreted to represent the amount of scaling
	  *         applied to the <b>area</b> of space determined by this 2D transformation. If determinant is 1,
	  *         area is preserved. If determinant is 2, area is doubled. If determinant is 0, this transform will
	  *         yield points on a 1D line or on a 0D point. If determinant is negative, that means that this
	  *         transformation flips one of the axes (resulting area is mirrored horizontally or vertically).
	  */
	lazy val determinant = xTransform.x * yTransform.y - yTransform.x * xTransform.y
	
	/**
	  * @return An inverse of this matrix / transformation. When this matrix is multiplied with its inverse, that
	  *         yields an identity matrix.
	  */
	lazy val inverse =
	{
		if (determinant == 0.0)
			None
		else
		{
			Some(Matrix2D(
				apply(1, 1), -apply(1, 0),
				-apply(0, 1), apply(0, 0)
			) / determinant)
		}
	}
	
	/**
	  * @return A 3x3 matrix based on this matrix. The z-transformation matches that of the identity matrix (0, 0, 1)
	  */
	def to3D = Matrix3D(
		apply(0,0), apply(1,0), 0,
		apply(0,1), apply(1,1), 0,
		0, 0, 1
	)
	
	
	// IMPLEMENTED	----------------------------
	
	override def repr = this
	
	override val columns = Vector(xTransform, yTransform)
	
	override lazy val rows = Vector(Vector2D(xTransform.x, yTransform.x), Vector2D(xTransform.y, yTransform.y))
	
	override protected def buildCopy(columns: Vector[Vector2D]) =
	{
		val fullColumns = columns.padTo(2, Vector2D.zero)
		Matrix2D(fullColumns.head, fullColumns(1))
	}
	
	override protected def zeroDimension = Vector2D.zero
}

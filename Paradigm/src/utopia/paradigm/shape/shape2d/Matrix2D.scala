package utopia.paradigm.shape.shape2d

import utopia.flow.operator.equality.EqualsBy
import utopia.paradigm.angular.{DirectionalRotation, Rotation}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.enumeration.{Axis2D, RotationDirection}
import utopia.paradigm.shape.shape1d.vector.Vector1D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.{Dimensions, DimensionsWrapperFactory, HasDimensions, MatrixLike}
import utopia.paradigm.transform.{AffineTransformable, JavaAffineTransformConvertible, LinearTransformable}

import java.awt.geom.AffineTransform

// See: https://en.wikipedia.org/wiki/Transformation_matrix
object Matrix2D extends DimensionsWrapperFactory[Vector2D, Matrix2D]
{
	// ATTRIBUTES	---------------------------
	
	/**
	  * A 2D transformation that keeps the vectors as is (I * V = V)
	  */
	val identity = apply(
		1, 0,
		0, 1
	)
	// cos -sin sin cos
	// cos = 0, sin = 1
	/**
	  * A linear rotation transformation that rotates items 90 degrees clockwise
	  */
	lazy val quarterRotationClockwise = apply(
		0, -1,
		1, 0)
	/**
	  * A linear rotation transformation that rotates items 90 degrees counter-clockwise
	  */
	lazy val quarterRotationCounterClockwise = apply(
		0, 1,
		-1, 0)
	/**
	  * A linear rotation transformation that rotates items 180 degrees
	  */
	lazy val rotation180Degrees = rotation(Rotation.clockwise.degrees(180))
	
	
	// IMPLEMENTED  ---------------------------
	
	override def zeroDimension = Vector2D.zero
	
	override def apply(dimensions: Dimensions[Vector2D]) = new Matrix2D(dimensions.withLength(2))
	
	override def from(other: HasDimensions[Vector2D]) = other match {
		case m: Matrix2D => m
		case o => apply(o.dimensions)
	}
	
	
	// OTHER	-------------------------------
	
	/**
	  * Creates a new matrix by providing the numbers from left to right, up to down
	  * @param xx X-component of the x-transformation
	  * @param yx X-component of the y-transformation
	  * @param xy Y-component of the x-transformation
	  * @param yy Y-component of the y-transformation
	  * @return A matrix that consists of the two transformations
	  */
	def apply(xx: Double, yx: Double, xy: Double, yy: Double): Matrix2D = apply(Vector2D(xx, xy), Vector2D(yx, yy))
	
	/**
	  * Creates a new linear scaling transformation matrix
	  * @param xScaling Scaling applied to x-axis coordinates
	  * @param yScaling Scaling applied to y-axis coordinates
	  * @return A scaling transformation matrix
	  */
	def scaling(xScaling: Double, yScaling: Double) = apply(
		xScaling, 0,
		0, yScaling)
	/**
	  * Creates a linear scaling transformation matrix
	  * @param modifier A scaling modifier applied to both x and y axes
	  * @return A new scaling transformation matrix
	  */
	def scaling(modifier: Double): Matrix2D = scaling(modifier, modifier)
	/**
	  * @param modifier Scaling modifier to apply
	  * @param axis The axis to target with the scaling
	  * @return A new linear scaling transformation
	  */
	def scaling(modifier: Double, axis: Axis2D): Matrix2D = axis match {
		case X => scaling(modifier, 1)
		case Y => scaling(1, modifier)
	}
	/**
	  * Creates a linear scaling transformation matrix
	  * @param vector A vector containing scaling modifiers for x and y axes
	  * @return A new scaling transformation matrix
	  */
	def scaling(vector: HasDoubleDimensions): Matrix2D = scaling(vector.x, vector.y)
	/**
	  * @param vector A one-dimensional vector
	  * @return A scaling that affects only the vector's axis, based on the vector's length
	  */
	def scaling(vector: Vector1D): Matrix2D = vector.axis match {
		case X => scaling(vector.length, 1)
		case Y => scaling(1, vector.length)
		case _ => scaling(1, 1)
	}
	
	/**
	  * Creates a new linear rotation transformation matrix
	  * @param amount Rotation amount
	  * @return A rotation transformation matrix that rotates items by the specified amount
	  */
	// See: https://en.wikipedia.org/wiki/Rotation_matrix
	// NB: In Wikipedia, the positive direction is counterclockwise. Here it is clockwise.
	//     However, this version seems to work...
	def rotation(amount: DirectionalRotation) = {
		val cos = amount.cosine
		val sin = amount.sine
		apply(
			cos, -sin,
			sin, cos)
	}
	/**
	  * @param direction Applied rotation direction
	  * @return A matrix that rotates the items 90 degrees towards the specified direction
	  */
	def quarterRotationTowards(direction: RotationDirection) = direction match {
		case Clockwise => quarterRotationClockwise
		case Counterclockwise => quarterRotationCounterClockwise
	}
	
	/**
	  * Creates a new linear shearing transformation matrix
	  * @param xShearing Shearing along x-axis
	  * @param yShearing Shearing along y-axis
	  * @return A shearing transformation matrix
	  */
	def shearing(xShearing: Double, yShearing: Double) = apply(
		1, xShearing,
		yShearing, 1)
	/**
	  * @param amount Shearing amount (X & Y)
	  * @return A new linear shearing transformation matrix
	  */
	def shearing(amount: HasDoubleDimensions): Matrix2D = shearing(amount.x, amount.y)
}

/**
  * Represents a 2x2 matrix, which can be visualized as a 2-dimensional transformation
  * @author Mikko Hilpinen
  * @since Genesis 15.7.2020, v2.3
  */
class Matrix2D private(override val columns: Dimensions[Vector2D])
	extends MatrixLike[Vector2D, Matrix2D] with LinearTransformable[Matrix2D]
		with AffineTransformable[Matrix3D] with JavaAffineTransformConvertible with EqualsBy
{
	// ATTRIBUTES   ----------------------------
	
	// [[x1, y1], [x2, y2]] => det = x1*y2 - y1*x2, kind of a cross between this matrix
	/**
	  * @return The determinant of this matrix. Determinant can be interpreted to represent the amount of scaling
	  *         applied to the <b>area</b> of space determined by this 2D transformation. If determinant is 1,
	  *         area is preserved. If determinant is 2, area is doubled. If determinant is 0, this transform will
	  *         yield points on a 1D line or on a 0D point. If determinant is negative, that means that this
	  *         transformation flips one of the axes (resulting area is mirrored horizontally or vertically).
	  */
	override lazy val determinant = xTransform.x * yTransform.y - yTransform.x * xTransform.y
	
	/**
	  * @return An inverse of this matrix / transformation. When this matrix is multiplied with its inverse, that
	  *         yields an identity matrix.
	  */
	override lazy val inverse = {
		if (determinant == 0.0)
			None
		else {
			Some(Matrix2D(
				apply(1, 1), -apply(1, 0),
				-apply(0, 1), apply(0, 0)
			) / determinant)
		}
	}
	
	override lazy val rows = Dimensions(Vector2D.zero)(
		Vector2D(xTransform.x, yTransform.x),
		Vector2D(xTransform.y, yTransform.y)
	)
	
	
	// COMPUTED	--------------------------------
	
	/**
	  * @return A 3x3 matrix based on this matrix. The z-transformation matches that of the identity matrix (0, 0, 1)
	  */
	def to3D = Matrix3D(
		apply(0,0), apply(1,0), 0,
		apply(0,1), apply(1,1), 0,
		0, 0, 1
	)
	
	/**
	  * @return Whether this matrix simply scales both x and y axes equally, when used as a linear transformation.
	  *         False if this transformation contains shearing or is unequal between the two axes.
	  */
	def isEqualScaling = apply(1, 0) == 0 && apply(0, 1) == 0 && apply(0, 0) == apply(1, 1)
	
	
	// IMPLEMENTED	----------------------------
	
	override def self = this
	override def identity: Matrix2D = this
	override def affineIdentity: Matrix3D = to3D
	
	override protected def equalsProperties = dimensions
	
	// This matrix uses coordinates (column (x), row (y))
	// Affine transform uses coordinates (row (y), column (x))
	override def toJavaAffineTransform = new AffineTransform(
		apply(0, 0), apply(0, 1),
		apply(1, 0), apply(1, 1), 0, 0)
	
	override def withDimensions(newDimensions: Dimensions[Vector2D]) = Matrix2D(newDimensions)
	
	override def transformedWith(transformation: Matrix2D) = transformation.apply(this)
	override def transformedWith(transformation: Matrix3D) = transformation(to3D)
	
	
	// OTHER	---------------------------------
	
	/**
	  * Transforms the specified instance
	  * @param transformable A transformable instance
	  * @tparam A Type of resulting item
	  * @return A transformed copy of the specified instance
	  */
	def transform[A](transformable: LinearTransformable[A]) = transformable.transformedWith(this)
}

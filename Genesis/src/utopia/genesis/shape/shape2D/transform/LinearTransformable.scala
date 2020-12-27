package utopia.genesis.shape.shape2D.transform

import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.Rotation
import utopia.genesis.shape.shape2D.{Matrix2D, Vector2DLike}

/**
  * A common trait for shapes etc. which can be transformed using linear transformations
  * @author Mikko Hilpinen
  * @since 25.12.2020, v2.4
  */
trait LinearTransformable[+Transformed]
{
	// ABSTRACT	---------------------------
	
	/**
	  * @param transformation A linear transformation (matrix) to apply
	  * @return A transformed copy of this instance
	  */
	def transformedWith(transformation: Matrix2D): Transformed
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @param transformation A linear transformation (matrix) to apply
	  * @return A transformed copy of this instance
	  */
	def *(transformation: Matrix2D) = transformedWith(transformation)
	
	/**
	  * @param transformation A linear transformation to apply to this instance
	  * @return A transformed copy of this instance
	  */
	def *(transformation: LinearTransformation) = transformedWith(transformation.toMatrix)
	
	/**
	  * @return A copy of this instance that has been rotated 90 degrees clockwise
	  */
	def rotated90DegreesClockwise = transformedWith(Matrix2D.quarterRotationClockwise)
	
	/**
	  * @return A copy of this instance that has been rotated 90 degrees counter-clockwise
	  */
	def rotated90DegreesCounterClockwise = transformedWith(Matrix2D.quarterRotationCounterClockwise)
	
	/**
	  * @return A copy of this instance that has been rotated 180 degrees
	  */
	def rotated180Degrees = transformedWith(Matrix2D.rotation180Degrees)
	
	/**
	  * @return A copy of this instance that has been flipped along the x-axis
	  */
	def flippedHorizontally = scaled(-1, 1)
	
	/**
	  * @return A copy of this instance that has been flipped along the y-axis
	  */
	def flippedVertically = scaled(1, -1)
	
	
	// OTHER	----------------------------
	
	/**
	  * @param xScaling Scaling to apply along the x-axis
	  * @param yScaling Scaling to apply along the y-axis
	  * @return A scaled copy of this instance
	  */
	def scaled(xScaling: Double, yScaling: Double) = transformedWith(Matrix2D.scaling(xScaling, yScaling))
	
	/**
	  * @param modifier A scaling modifier to apply
	  * @return A scaled copy of this instance
	  */
	def scaled(modifier: Double) = transformedWith(Matrix2D.scaling(modifier))
	
	/**
	  * @param vector A vector that represents x- and y-scaling
	  * @return A scaled copy of this instance
	  */
	def scaled(vector: Vector2DLike[_]): Transformed = scaled(vector.x, vector.y)
	
	/**
	  * @param axis     Target axis
	  * @param modifier Scaling amount applied along that axis
	  * @return A scaled copy of this instance
	  */
	def scaledAlong(axis: Axis2D, modifier: Double) = transformedWith(Matrix2D.scaling(modifier, axis))
	
	/**
	  * @param amount Rotation to apply to this instance
	  * @return A rotated copy of this instance
	  */
	def rotated(amount: Rotation) = transformedWith(Matrix2D.rotation(amount))
}

package utopia.genesis.shape.shape2D.transform

import utopia.genesis.shape.shape2D.Vector2DLike
import utopia.genesis.shape.shape3D.Matrix3D

/**
  * A common trait for shapes etc. that can be transformed using 2D affine transformations
  * @author Mikko Hilpinen
  * @since 25.12.2020, v2.4
  */
trait AffineTransformable[+Transformed]
{
	// ABSTRACT	--------------------------
	
	/**
	  * @param transformation An affine transformation matrix to apply to this instance
	  * @return A transformed copy of this instance
	  */
	def transformedWith(transformation: Matrix3D): Transformed
	
	
	// OTHER	--------------------------
	
	/**
	  * @param transformation An affine transformation matrix to apply to this instance
	  * @return A transformed copy of this instance
	  */
	def *(transformation: Matrix3D) = transformedWith(transformation)
	
	/**
	  * @param transformation Transformation to apply to this instance
	  * @return A transformed copy of this instance
	  */
	def *(transformation: AffineTransformation) = transformedWith(transformation.toMatrix)
	
	/**
	  * @param translation Amount of translation to apply
	  * @return A translated copy of this instance
	  */
	def translated(translation: Vector2DLike[_]) = transformedWith(Matrix3D.translation(translation))
}

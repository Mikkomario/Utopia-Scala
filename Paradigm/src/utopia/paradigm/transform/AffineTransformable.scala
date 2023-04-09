package utopia.paradigm.transform

import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

/**
  * A common trait for shapes etc. that can be transformed using 2D affine transformations
  * @author Mikko Hilpinen
  * @since Genesis 25.12.2020, v2.4
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
	  * @param transformation A transformation to apply to this item
	  * @return A transformed copy of this item
	  */
	def transformedWith(transformation: AffineTransformation): Transformed = transformedWith(transformation.toMatrix)
	
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
	def translated(translation: HasDoubleDimensions) = transformedWith(Matrix3D.translation(translation))
}

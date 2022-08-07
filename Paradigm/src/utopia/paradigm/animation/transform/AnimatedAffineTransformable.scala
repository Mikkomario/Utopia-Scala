package utopia.paradigm.animation.transform

import utopia.paradigm.animation.Animation
import utopia.paradigm.shape.shape2d.Vector2DLike
import utopia.paradigm.shape.shape3d.Matrix3D

/**
  * A common trait for instances that support animated affine transformations
  * @author Mikko Hilpinen
  * @since Genesis 26.12.2020, v2.4
  */
trait AnimatedAffineTransformable[+Transformed]
{
	// ABSTRACT	------------------------------
	
	/**
	  * @param transformation An animated transformation to apply over this instance
	  * @return A transformed (animated) copy of this instance
	  */
	def affineTransformedWith(transformation: Animation[Matrix3D]): Transformed
	
	
	// OTHER	-----------------------------
	
	def translatedOverTime[V <: Vector2DLike[V]](translation: V) =
		affineTransformedWith(AnimatedAffineTransformation.translate(translation))
}

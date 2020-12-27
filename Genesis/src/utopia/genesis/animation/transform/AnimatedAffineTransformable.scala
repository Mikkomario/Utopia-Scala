package utopia.genesis.animation.transform

import utopia.genesis.animation.Animation
import utopia.genesis.shape.shape2D.Vector2DLike
import utopia.genesis.shape.shape3D.Matrix3D

/**
  * A common trait for instances that support animated affine transformations
  * @author Mikko Hilpinen
  * @since 26.12.2020, v2.4
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

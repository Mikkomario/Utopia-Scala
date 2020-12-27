package utopia.genesis.animation.transform

import utopia.genesis.animation.Animation
import utopia.genesis.shape.shape2D.transform.{AffineTransformable, LinearTransformable}
import utopia.genesis.shape.shape2D.{Matrix2D, Vector2DLike}
import utopia.genesis.shape.shape3D.Matrix3D

object AnimatedAffineTransformation
{
	/**
	  * Creates a translation animation
	  * @param amount Translation amount as a vector
	  * @tparam V Type of translation vector
	  * @return A new animated transformation
	  */
	def translate[V <: Vector2DLike[V]](amount: V) =
		apply { p => Matrix3D.translation(amount * p) }
}

/**
  * Creates an animation by transforming items with affine transformations
  * @author Mikko Hilpinen
  * @since 26.12.2020, v2.4
  */
case class AnimatedAffineTransformation(f: Double => Matrix3D)
	extends Animation[Matrix3D] with LinearTransformable[AnimatedAffineTransformation]
		with AffineTransformable[AnimatedAffineTransformation]
		with AnimatedLinearTransformable[AnimatedAffineTransformation]
		with AnimatedAffineTransformable[AnimatedAffineTransformation]
{
	// IMPLEMENTED	------------------------------
	
	override def apply(progress: Double) = f(progress)
	
	override def transformedWith(transformation: Matrix2D) =
		AnimatedAffineTransformation { p => f(p) * transformation }
	
	override def transformedWith(transformation: Matrix3D) =
		AnimatedAffineTransformation { p => transformation(f(p)) }
	
	override def transformedWith(transformation: Animation[Matrix2D]) =
		AnimatedAffineTransformation { p => f(p) * transformation(p) }
	
	override def affineTransformedWith(transformation: Animation[Matrix3D]) =
		AnimatedAffineTransformation { p => transformation(p)(apply(p)) }
	
	
	// OTHER	----------------------------------
	
	/**
	  * @param transformable An instance to transform
	  * @tparam A Type of transformation result
	  * @return Transformation result
	  */
	def transform[A](transformable: AnimatedAffineTransformable[A]) = transformable.affineTransformedWith(this)
}

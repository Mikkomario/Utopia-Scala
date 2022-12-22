package utopia.paradigm.animation.transform

import utopia.paradigm.animation.Animation
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.transform.{AffineTransformable, LinearTransformable}

object AnimatedAffineTransformation
{
	/**
	  * Creates a translation animation
	  * @param amount Translation amount as a vector
	  * @return A new animated transformation
	  */
	def translate(amount: HasDoubleDimensions) =
		apply { p => Matrix3D.translation(amount.dimensions.map { _ * p }) }
}

/**
  * Creates an animation by transforming items with affine transformations
  * @author Mikko Hilpinen
  * @since Genesis 26.12.2020, v2.4
  */
case class AnimatedAffineTransformation(f: Double => Matrix3D)
	extends Animation[Matrix3D] with LinearTransformable[AnimatedAffineTransformation]
		with AffineTransformable[AnimatedAffineTransformation]
		with AnimatedLinearTransformable[AnimatedAffineTransformation]
		with AnimatedAffineTransformable[AnimatedAffineTransformation]
{
	// IMPLEMENTED	------------------------------
	
	override def self = this
	
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
	def transform[A](transformable: AnimatedAffineTransformable[A]) =
		transformable.affineTransformedWith(this)
}

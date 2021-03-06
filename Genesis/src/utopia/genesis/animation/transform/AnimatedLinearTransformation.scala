package utopia.genesis.animation.transform

import utopia.genesis.animation.Animation
import utopia.genesis.shape.shape1D.Rotation
import utopia.genesis.shape.shape2D.transform.{AffineTransformable, LinearTransformable}
import utopia.genesis.shape.shape2D.{Matrix2D, Vector2DLike}
import utopia.genesis.shape.shape3D.Matrix3D

object AnimatedLinearTransformation
{
	// ATTRIBUTES	-----------------------
	
	/**
	  * An animation that rotates objects 360 degrees clockwise
	  */
	val rotate360Clockwise = rotation(Rotation.clockwiseCircle)
	
	/**
	  * An animation that rotates objects 360 degrees counter-clockwise
	  */
	val rotate360Counterclockwise = rotation(Rotation.counterclockwiseCircle)
	
	/**
	  * An animation that flips objects horizontally
	  */
	val flipHorizontally = scaling(-1, 1)
	
	/**
	  * An animation that flips objects vertically
	  */
	val flipVertically = scaling(1, -1)
	
	
	// OTHER	---------------------------
	
	/**
	  * Creates a rotation animation
	  * @param amount Rotation amount
	  * @return An animation that rotates the specified amount over time
	  */
	def rotation(amount: Rotation) = apply { p => Matrix2D.rotation(amount * p) }
	
	/**
	  * Creates a scaling animation
	  * @param amount Scaling amount
	  * @return An animation that scales the specified amount over time
	  */
	def scaling(amount: Double) =
	{
		val change = amount - 1
		apply { p => Matrix2D.scaling(1 + change * p) }
	}
	
	/**
	  * Creates a scaling animation
	  * @param xScaling Scaling amount to apply over the x-axis
	  * @param yScaling Scaling amount to apply over the y-axis
	  * @return An animation that scales the specified amount over time
	  */
	def scaling(xScaling: Double, yScaling: Double) =
	{
		val xChange = xScaling - 1
		val yChange = yScaling - 1
		apply { p => Matrix2D.scaling(1 + xChange * p, 1 + yChange * p) }
	}
	
	/**
	  * Creates a scaling animation
	  * @param amount Scaling amount
	  * @return An animation that scales the specified amount over time
	  */
	def scaling(amount: Vector2DLike[_]): AnimatedLinearTransformation = scaling(amount.x, amount.y)
	
	/**
	  * Creates a shearing animation
	  * @param xShear Shearing to apply along x-axis
	  * @param yShear Shearing to apply along y-axis
	  * @return An animation that shears the specified amount over time
	  */
	def shearing(xShear: Double, yShear: Double) =
		apply { p => Matrix2D.shearing(xShear * p, yShear * p) }
	
	/**
	  * Creates a shearing animation
	  * @param amount Shearing amount
	  * @return An animation that shears the specified amount over time
	  */
	def shearing(amount: Vector2DLike[_]): AnimatedLinearTransformation = shearing(amount.x, amount.y)
}

/**
  * Creates an animation by transforming items using linear transformations
  * @param f A transformation animation function
  * @author Mikko Hilpinen
  * @since 26.12.2020, v2.4
  */
case class AnimatedLinearTransformation(f: Double => Matrix2D) extends Animation[Matrix2D]
	with LinearTransformable[AnimatedLinearTransformation] with AnimatedLinearTransformable[AnimatedLinearTransformation]
	with AffineTransformable[AnimatedAffineTransformation] with AnimatedAffineTransformable[AnimatedAffineTransformation]
{
	// IMPLEMENTED	-------------------------
	
	override def apply(progress: Double) = f(progress)
	
	override def transformedWith(transformation: Matrix2D) =
		AnimatedLinearTransformation { progress => transformation(apply(progress)) }
	
	override def transformedWith(transformation: Animation[Matrix2D]) =
		AnimatedLinearTransformation { progress => transformation(progress)(apply(progress)) }
	
	override def transformedWith(transformation: Matrix3D) =
		AnimatedAffineTransformation { p => transformation(f(p)) }
	
	override def affineTransformedWith(transformation: Animation[Matrix3D]) =
		AnimatedAffineTransformation { p => transformation(p)(f(p)) }
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param transformable An instance to transform
	  * @tparam A Transformation result
	  * @return Transformation result
	  */
	def transform[A](transformable: AnimatedLinearTransformable[A]) = transformable.transformedWith(this)
}

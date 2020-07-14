package utopia.genesis.animation

import utopia.genesis.shape.shape1D.Rotation
import utopia.genesis.shape.shape1D.RotationDirection.Counterclockwise
import utopia.genesis.shape.shape2D.{Point, Transformation}
import utopia.genesis.shape.shape3D.Vector3D

/**
  * Used for constructing transformation animations
  * @author Mikko Hilpinen
  * @since 15.6.2020, v2.3
  */
object AnimatedTransformation
{
	// COMPUTED	------------------------
	
	/**
	  * An animation where the target is rotated 360 degrees clockwise
	  */
	val rotated360Clockwise = rotation()
	
	/**
	  * An animation where the target is rotated 360 degress counter-clockwise
	  */
	val rotated360CounterClockwise = rotation(Rotation.ofCircles(1, Counterclockwise))
	
	/**
	  * An animation where the target is flipped horizontally
	  */
	val flippedHorizontally = scaling(Vector3D.identity, Vector3D(-1, 1))
	
	/**
	  * An animation where the target is flipped vertically
	  */
	val flippedVertically = scaling(Vector3D.identity, Vector3D(1, -1))
	
	
	// OTHER	------------------------
	
	/**
	  * @param amount Amount of rotation to apply
	  * @param base The starting rotation
	  * @return A new rotation transformation
	  */
	def rotation(amount: Rotation = Rotation.ofCircles(1), base: Rotation = Rotation.zero) =
		apply { progress => Transformation.rotation(base + amount * progress) }
	
	/**
	  * @param from Scaling at 0.0 progress
	  * @param to Scaling at 1.0 progress
	  * @return A new scaling transformation
	  */
	def scaling(from: Double, to: Double) =
		apply { p => Transformation.scaling(from + (to - from) * p) }
	
	/**
	  * @param from Scaling at 0.0 progress
	  * @param to Scaling at 1.0 progress
	  * @return A new scaling transformation
	  */
	def scaling(from: Vector3D, to: Vector3D) =
		apply { p  => Transformation.scaling(from + (to - from) * p) }
	
	/**
	  * @param path Path that determines translation / position
	  * @return A transformation animation that follows the specified path
	  */
	def moving(path: Animation[Point]) = apply { p => Transformation.position(path(p)) }
}

case class AnimatedTransformation(f: Double => Transformation) extends Animation[Transformation]
{
	// IMPLEMENTED	-------------------------
	
	override def apply(progress: Double) = f(progress)
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param another Another animated transformation
	  * @return A simultaneous combination of these two transformations
	  */
	def +(another: Animation[Transformation]) = animatedTransformed { (t, p) => t + another(p) }
	
	/**
	  * @param translation Amount of translation applied (static)
	  * @return A translated version of this animation
	  */
	def translated(translation: Vector3D) = transformed { _.translated(translation) }
	
	/**
	  * @param translation Amount of translation applied (animated)
	  * @return A translated version of this animation
	  */
	def translated(translation: Animation[Vector3D]) = animatedTransformed { (t, p) =>
		t.translated(translation(p)) }
	
	/**
	  * @param rotationTransform Amount of rotation applied (animated)
	  * @return A rotated version of this animation
	  */
	def rotated(rotationTransform: Animation[Rotation]) = animatedTransformed { (t, p) =>
		t.rotated(rotationTransform(p)) }
	
	/**
	  * @param rotation Amount of rotation applied (static)
	  * @return A rotated version of this animation
	  */
	def rotated(rotation: Rotation) = transformed { _.rotated(rotation) }
	
	/**
	  * @param scaling Amount of scaling applied (static)
	  * @return A scaled version of this animation
	  */
	def scaled(scaling: Vector3D) = transformed { _.scaled(scaling) }
	
	/**
	  * @param scaling Amount of scaling applied (static)
	  * @return A scaled version of this animation
	  */
	def scaled(scaling: Double) = transformed { _.scaled(scaling) }
	
	/**
	  * @param scaling Amount of scaling applied (animation)
	  * @return A scaled version of this animation
	  */
	def scaled(scaling: Animation[Vector3D]) = animatedTransformed { (t, p) => t.scaled(scaling(p)) }
	
	private def transformed(t: Transformation => Transformation) =
		AnimatedTransformation { p => t(f(p)) }
	
	private def animatedTransformed(t: (Transformation, Double) => Transformation) =
		AnimatedTransformation { p => t(f(p), p) }
}

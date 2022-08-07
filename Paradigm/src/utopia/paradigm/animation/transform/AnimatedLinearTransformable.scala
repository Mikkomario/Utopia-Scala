package utopia.paradigm.animation.transform

import utopia.paradigm.angular.Rotation
import utopia.paradigm.animation.Animation
import utopia.paradigm.shape.shape2d.Matrix2D

/**
  * A common trait for instances that can be transformed with animated linear transformations
  * @author Mikko Hilpinen
  * @since Genesis 26.12.2020, v2.4
  */
trait AnimatedLinearTransformable[+Transformed]
{
	// ABSTRACT	------------------------------
	
	/**
	  * Transforms this instance using an animated transformation
	  * @param transformation Transformation to apply to this instance
	  * @return A transformed & animated copy of this instance
	  */
	def transformedWith(transformation: Animation[Matrix2D]): Transformed
	
	
	// COMPUTED	------------------------------
	
	/**
	  * @return An animated transformed copy of this instance, being rotated 360 degrees clockwise
	  */
	def rotated360ClockwiseOverTime = transformedWith(AnimatedLinearTransformation.rotate360Clockwise)
	
	/**
	  * @return An animated transformed copy of this instance, being rotated 360 degrees counter-clockwise
	  */
	def rotated360CounterclockwiseOverTime = transformedWith(AnimatedLinearTransformation.rotate360Counterclockwise)
	
	
	// OTHER	------------------------------
	
	/**
	  * Transforms this instance using an animated transformation
	  * @param transformation Transformation to apply to this instance
	  * @return A transformed & animated copy of this instance
	  */
	def *(transformation: Animation[Matrix2D]) = transformedWith(transformation)
	
	/**
	  * @param rotation A rotation animation to apply to this instance
	  * @return An animated transformed copy of this instance
	  */
	def rotatedOverTime(rotation: Rotation) = transformedWith(AnimatedLinearTransformation.rotation(rotation))
	
	/**
	  * @param scaling A scaling animation to apply to this instance
	  * @return An animated transformed copy of this instance
	  */
	def scaledOverTime(scaling: Double) = transformedWith(AnimatedLinearTransformation.scaling(scaling))
	
	/**
	  * @param xScaling An x-wise scaling animation to apply to this instance
	  * @param yScaling An y-wise scaling animation to apply to this instance
	  * @return An animated transformed copy of this instance
	  */
	def scaledOverTime(xScaling: Double, yScaling: Double) =
		transformedWith(AnimatedLinearTransformation.scaling(xScaling, yScaling))
	
	/**
	  * @param xShear x-wise shearing to apply over time
	  * @param yShear y-wise shearing to apply over time
	  * @return An animated transformed copy of this instance
	  */
	def shearedOverTime(xShear: Double, yShear: Double) = transformedWith(AnimatedLinearTransformation.shearing(xShear, yShear))
}

package utopia.genesis.animation.transform

import utopia.genesis.animation.Animation
import utopia.genesis.animation.transform.AnimatedTransform.{AndTransform, StaticTransform}

import scala.concurrent.duration.Duration

/**
  * Animations are transformations that can be repeated and are applied over time
  * @author Mikko Hilpinen
  * @since 11.8.2019, v2.1+
  */
trait AnimatedTransform[-Origin, +Reflection]
{
	// ABSTRACT	----------------------
	
	/**
	  * Transforms an item based on this animation and specified progress
	  * @param original The original item
	  * @param progress The progress on this animation [0, 1] where 0 is the beginning and 1 is the end
	  * @return Transformed item
	  */
	def apply(original: Origin, progress: Double): Reflection
	
	
	// OTHER	----------------------
	
	/**
	  * Combines this animated transform with another transform so that both are applied (this transform is applied first)
	  * @param other Another transform
	  * @tparam NewReflection Result type of the provided transformation
	  * @return A new transformation that applies both of these transformations (first this, then other)
	  */
	def &&[NewReflection](other: AnimatedTransform[Reflection, NewReflection]): AnimatedTransform[Origin, NewReflection] =
		AndTransform(this, other)
	
	/**
	  * @param duration Target duration
	  * @return A copy of this transform that is spanned over specified duration
	  */
	def over(duration: Duration) = TimedTransform.wrap(this, duration)
	
	/**
	  * @param origin A static origin for this transformation
	  * @return An animation where the origin is transformed using this transform
	  */
	def toAnimation(origin: Origin): Animation[Reflection] = new StaticTransform(origin, this)
}

object AnimatedTransform
{
	// NESTED	----------------------
	
	private class StaticTransform[-Origin, +Reflection](getOrigin: => Origin, transform: AnimatedTransform[Origin, Reflection])
		extends Animation[Reflection]
	{
		override def apply(progress: Double) = transform(getOrigin, progress)
	}
	
	private case class AndTransform[-Origin, MidPoint, +Reflection]
	(first: AnimatedTransform[Origin, MidPoint], second: AnimatedTransform[MidPoint, Reflection])
		extends AnimatedTransform[Origin, Reflection]
	{
		override def apply(original: Origin, progress: Double) = second(first(original, progress), progress)
	}
}

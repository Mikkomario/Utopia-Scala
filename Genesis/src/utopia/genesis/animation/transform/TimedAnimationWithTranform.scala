package utopia.genesis.animation.transform

import utopia.genesis.animation.{Animation, TimedAnimation}

import scala.concurrent.duration.Duration

/**
  * This animation applies an animated transformation, and also has a specified duration
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
trait TimedAnimationWithTranform[Origin, +Reflection] extends AnimationWithTransform[Origin, Reflection]
	with TimedAnimation[Reflection]

object TimedAnimationWithTranform
{
	// OTHER	--------------------------
	
	/**
	  * Wraps a timed animation and a non-timed transform to form a single animation
	  * @param animation Animation to wrap (timed)
	  * @param transform Transform to apply (not timed)
	  * @tparam O Type of original animation
	  * @tparam R Type of transformation result
	  * @return A copy of the specified animation with a transformation applied over that animation's duration
	  */
	def wrapTimedAnimation[O, R](animation: TimedAnimation[O], transform: AnimatedTransform[O, R]): TimedAnimationWithTranform[O, R] =
		new TimedAnimationWithTransformWrapper(animation, transform, animation.duration)
	
	/**
	  * Wraps a non-timed animation and a timed animated transformation to form a single timed animation
	  * @param animation Animation to wrap (not timed)
	  * @param transform Transform to apply (timed)
	  * @tparam O Type of original animation
	  * @tparam R Type of transformation result
	  * @return A new timed animation that combines the original animation with a transform and uses the transform's duration
	  */
	def wrapTimedTransform[O, R](animation: Animation[O], transform: TimedTransform[O, R]): TimedAnimationWithTranform[O, R] =
		new TimedAnimationWithTransformWrapper(animation, transform, transform.duration)
	
	
	// NESTED	--------------------------
	
	private class TimedAnimationWithTransformWrapper[O, +R](animation: Animation[O], override val transform: AnimatedTransform[O, R],
															getDuration: => Duration) extends TimedAnimationWithTranform[O, R]
	{
		override def raw(progress: Double) = animation(progress)
		
		override def duration = getDuration
	}
}

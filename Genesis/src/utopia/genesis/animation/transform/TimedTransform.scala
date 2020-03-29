package utopia.genesis.animation.transform

import utopia.genesis.animation.TimedAnimation
import utopia.genesis.animation.transform.TimedTransform.StaticTimedTransform

import scala.concurrent.duration.Duration

/**
  * This transformation has a time element
  * @author Mikko Hilpinen
  * @since 18.8.2019, v2.1+
  */
trait TimedTransform[-Origin, +Reflection] extends AnimatedTransform[Origin, Reflection]
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return The duration of this transform
	  */
	def duration: Duration
	
	
	// IMPLEMENTED	---------------------
	
	override def toAnimation(origin: Origin): TimedAnimation[Reflection] = new StaticTimedTransform(origin, this)
	
	
	// OTHER	-------------------------
	
	/**
	  * @param original Original animated instance
	  * @param passedTime Amount of passed time since animation start
	  * @return Animation's progress at specified time
	  */
	def apply(original: Origin, passedTime: Duration): Reflection = apply(original, passedTime / duration)
	
	/**
	  * @param original Original animated instance
	  * @param passedTime Amount of passed time since animation start
	  * @return Animation's progress at specified time. If time is larger than the duration of this transform,
	  *         this transform is repeated.
	  */
	def repeating(original: Origin, passedTime: Duration) =
	{
		val d = duration.toNanos
		apply(original, (passedTime.toNanos % d) / d.toDouble)
	}
}

object TimedTransform
{
	// OTHER	------------------------
	
	/**
	  * Wraps another animated transform and spans it over specified duration
	  * @param transform Transform to wrap
	  * @param duration Duration over which the transformation is spanned
	  * @tparam O Type of transformation origin
	  * @tparam R Type of transformation result
	  * @return A timed transformation that uses specified transformation
	  */
	def wrap[O, R](transform: AnimatedTransform[O, R], duration: Duration): TimedTransform[O, R] =
		new TimedTransformWrapper(transform, duration)
	
	
	// NESTED	------------------------
	
	private class StaticTimedTransform[-Origin, +Reflection](origin: Origin, transform: TimedTransform[Origin, Reflection])
		extends TimedAnimation[Reflection]
	{
		override def duration = transform.duration
		
		override def apply(progress: Double) = transform(origin, progress)
	}
	
	private class TimedTransformWrapper[-Origin, +Reflection](wrapped: AnimatedTransform[Origin, Reflection],
															  override val duration: Duration)
		extends TimedTransform[Origin, Reflection]
	{
		override def apply(original: Origin, progress: Double) = wrapped(original, progress)
	}
}

package utopia.genesis.animation

import utopia.genesis.animation.TimedAnimation.{CurvedAnimation, MapAnimation, RepeatingAnimation, ReverseAnimation}
import utopia.genesis.animation.transform.{AnimatedTransform, TimedAnimationWithTranform}

import scala.concurrent.duration.Duration

/**
  * A common trait for animations that have a time element
  * @author Mikko Hilpinen
  * @since 28.3.2020, v2.1
  */
trait TimedAnimation[+A] extends AnimationLike[A, TimedAnimation]
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return The duration of this transform
	  */
	def duration: Duration
	
	
	// COMPUTED ----------------------------
	
	def withReverseAppended: TimedAnimation[A] = appendedWith(reversed)
	
	
	// IMPLEMENTED	------------------------
	
	override def reversed: TimedAnimation[A] = new ReverseAnimation[A](this)
	
	override def map[B](f: A => B): TimedAnimation[B] = new MapAnimation(this)(f)
	
	override def curved(curvature: AnimationLike[Double, Any]): TimedAnimation[A] =
		new CurvedAnimation[A](this, curvature)
	
	override def repeated(times: Int): TimedAnimation[A] = new RepeatingAnimation[A](this, times)
	
	def transformedWith[O >: A, R](transform: AnimatedTransform[O, R]) =
		TimedAnimationWithTranform.wrapTimedAnimation[O, R](this, transform)
	
	
	// OTHER	----------------------------
	
	/**
	  * @param passedTime Amount of passed time since animation start
	  * @return Animation's progress at specified time
	  */
	def apply(passedTime: Duration): A = apply(passedTime / duration)
	
	/**
	  * @param passedTime Amount of passed time since animation start
	  * @return Animation's progress at specified time. If time is larger than the duration of this transform,
	  *         this animation is repeated.
	  */
	def repeating(passedTime: Duration) =
	{
		val d = duration.toNanos
		apply((passedTime.toNanos % d) / d.toDouble)
	}
	
	/**
	  * @param another Another animation
	  * @tparam B Type of the resulting animation result
	  * @return An animation that first plays this one and then the other
	  */
	def appendedWith[B >: A](another: TimedAnimation[B]) = TimedCombinedAnimation(this, another)
}

object TimedAnimation
{
	// OTHER	-----------------------
	
	/**
	  * Wraps another animation and gives it a duration
	  * @param animation Animation
	  * @param duration Duration for the animation
	  * @tparam A Type of animation result
	  * @return Provided animation with a duration
	  */
	def wrap[A](animation: Animation[A], duration: Duration): TimedAnimation[A] =
		new TimedAnimationWrapper[A](animation, duration)
	
	
	// NESTED	-----------------------
	
	private class CurvedAnimation[+A](wrapped: TimedAnimation[A], curve: AnimationLike[Double, Any])
		extends TimedAnimation[A]
	{
		override def duration = wrapped.duration
		
		override def apply(progress: Double) = wrapped(curve(progress))
	}
	
	private class TimedAnimationWrapper[+A](wrapped: Animation[A], override val duration: Duration) extends TimedAnimation[A]
	{
		override def apply(progress: Double) = wrapped(progress)
	}
	
	private class MapAnimation[A, +B](original: TimedAnimation[A])(f: A => B) extends TimedAnimation[B]
	{
		override def duration = original.duration
		
		override def apply(progress: Double) = f(original(progress))
	}
	
	private class RepeatingAnimation[+A](original: TimedAnimation[A], repeats: Int) extends TimedAnimation[A]
	{
		override def duration = original.duration * repeats
		
		override def apply(progress: Double) = original((progress * repeats) % 1)
	}
	
	private class ReverseAnimation[A](original: TimedAnimation[A]) extends TimedAnimation[A]
	{
		override def duration = original.duration
		
		override def apply(progress: Double) = original(1 - progress)
		
		override def reversed = original
	}
}
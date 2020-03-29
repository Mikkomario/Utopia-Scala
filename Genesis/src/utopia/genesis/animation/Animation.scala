package utopia.genesis.animation

import utopia.genesis.animation.Animation.{MapAnimation, RepeatingAnimation}
import utopia.genesis.animation.transform.{AnimatedTransform, AnimationWithTransform, TimedAnimationWithTranform, TimedTransform}

import scala.concurrent.duration.Duration

/**
  * Animations are items that have different states based on (time) progress
  * @author Mikko Hilpinen
  * @since 11.8.2019, v2.1+
  */
trait Animation[+A]
{
	// ABSTRACT	---------------------------
	
	/**
	  * Finds a state of this animation
	  * @param progress Progress over this animation
	  * @return The state of this animation at the specified point
	  */
	def apply(progress: Double): A
	
	
	// OTHER	--------------------------
	
	/**
	  * @param duration The new length (time-wise) for this animation
	  * @return This animation with specified duration
	  */
	def over(duration: Duration) = TimedAnimation.wrap(this, duration)
	
	/**
	  * @param transform Transformation applied
	  * @tparam O Type of transformation origin (must be super type of this animation's result)
	  * @tparam R Type of transformation result
	  * @return A copy of this animation with the specified transformation applied over it
	  */
	def transformedWith[O >: A, R](transform: AnimatedTransform[O, R]) =
		AnimationWithTransform.wrap[O, R](this, transform)
	
	/**
	  * @param transform Transformation applied (timed)
	  * @tparam O Type of transformation origin (must be super type of this animation's result)
	  * @tparam R Type of transformation result
	  * @return A copy of this animation with the specified transformation applied over it.
	  *         This animation is timed to complete at the same point with the transformation.
	  */
	def transformedOver[O >: A, R](transform: TimedTransform[O, R]) =
		TimedAnimationWithTranform.wrapTimedTransform[O, R](this, transform)
	
	/**
	  * Maps this animation
	  * @param f A mapping function. Please note that this function will be called multiple times.
	  * @tparam B Type of map result
	  * @return An animation that always provides the mapped value
	  */
	def map[B](f: A => B): Animation[B] = new MapAnimation(this)(f)
	
	/**
	  * Creates a new animation that repeats this one a number of times
	  * @param times The number of times this animation is repeated
	  * @return A new animation
	  */
	def repeated(times: Int): Animation[A] = new RepeatingAnimation[A](this, times)
}

object Animation
{
	// NESTED	---------------------------
	
	private class MapAnimation[A, +B](original: Animation[A])(f: A => B) extends Animation[B]
	{
		override def apply(progress: Double) = f(original(progress))
	}
	
	private class RepeatingAnimation[+A](original: Animation[A], repeats: Int) extends Animation[A]
	{
		override def apply(progress: Double) = original((progress * repeats) % 1)
	}
}
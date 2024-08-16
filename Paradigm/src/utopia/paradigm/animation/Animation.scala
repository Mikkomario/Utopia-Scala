package utopia.paradigm.animation

import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.paradigm.animation.Animation.{MapAnimation, MergeAnimation, RepeatingAnimation, ReverseAnimation}
import utopia.paradigm.animation.transform.{AnimatedTransform, AnimationWithTransform, TimedAnimationWithTranform, TimedTransform}

import scala.concurrent.duration.Duration

object Animation
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * An animation that progresses linearly from 0 to 1
	  */
	lazy val zeroToOne: Animation[Double] = IdentityAnimation
	/**
	  * An animation that progresses linearly from 1 to 0
	  */
	lazy val oneToZero: Animation[Double] = apply { 1 - _ }
	
	
	// OTHER	---------------------------
	
	/**
	  * @param f An animation function. Takes animation progress (usually between 0 and 1) and returns animation state
	  * @tparam A Type of animation result
	  * @return A new animation
	  */
	def apply[A](f: Double => A): Animation[A] = new FunctionAnimation[A](f)
	
	/**
	  * @param state A static state this animation will always have
	  * @tparam A Type of animation result
	  * @return An animation that always returns the same result
	  */
	def fixed[A](state: A) = new FixedAnimation[A](state)
	
	/**
	  * @param from Start value
	  * @param to End value
	  * @return An animation that progresses from the start value to the end value
	  */
	def progress(from: Double, to: Double): Animation[Double] = ProgressAnimation(from, to)
	/**
	  * @param range Progressed range of values
	  * @return An animation that progresses from the range start value to the range end value
	  */
	def progress(range: HasInclusiveEnds[Double]): Animation[Double] = progress(range.start, range.end)
	
	
	// NESTED	---------------------------
	
	private object IdentityAnimation extends Animation[Double]
	{
		override def apply(progress: Double): Double = progress
	}
	
	private class MapAnimation[A, +B](original: Animation[A])(f: A => B) extends Animation[B]
	{
		override def apply(progress: Double) = f(original(progress))
	}
	
	private class MergeAnimation[O1, O2, R](original1: AnimationLike[O1, Any], original2: AnimationLike[O2, Any])
	                                       (merge: (O1, O2) => R)
		extends Animation[R]
	{
		override def apply(progress: Double) = merge(original1(progress), original2(progress))
	}
	
	private class RepeatingAnimation[+A](original: Animation[A], repeats: Int) extends Animation[A]
	{
		override def apply(progress: Double) = original((progress * repeats) % 1)
	}
	
	private class FunctionAnimation[+A](f: Double => A) extends Animation[A]
	{
		override def apply(progress: Double) = f(progress)
	}
	
	private class ReverseAnimation[+A](original: Animation[A]) extends Animation[A]
	{
		override def apply(progress: Double) = original(1 - progress)
		
		override def reversed = original
	}
	
	private case class ProgressAnimation(from: Double, to: Double) extends Animation[Double]
	{
		// ATTRIBUTES   ------------------
		
		private val length = to - from
		
		
		// IMPLEMENTED  ------------------
		
		override def apply(progress: Double): Double = from + progress * length
	}
}

/**
  * Animations are items that have different states based on (time) progress
  * @author Mikko Hilpinen
  * @since Genesis 11.8.2019, v2.1+
  */
trait Animation[+A] extends AnimationLike[A, Animation]
{
	// COMPUTED --------------------------
	
	/**
	  * @return An animation that first plays this animation, and then the reverse version of this animation
	  */
	def withReverseAppended: Animation[A] = appendWith(reversed)
	
	
	// IMPLEMENTED  ----------------------
	
	/**
	  * @return A copy of this animation that has uses reversed progress
	  */
	override def reversed: Animation[A] = new ReverseAnimation[A](this)
	
	/**
	  * Creates a new animation that repeats this one a number of times
	  * @param times The number of times this animation is repeated
	  * @return A new animation
	  */
	def repeated(times: Int): Animation[A] = new RepeatingAnimation[A](this, times)
	
	/**
	  * @param curvature A curve animation used for transforming progress% values
	  * @return A curved version of this animation
	  */
	def curved(curvature: AnimationLike[Double, Any]): Animation[A] = CurvedAnimation(this, curvature)
	
	/**
	  * Maps this animation
	  * @param f A mapping function. Please note that this function will be called multiple times.
	  * @tparam B Type of map result
	  * @return An animation that always provides the mapped value
	  */
	override def map[B](f: A => B): Animation[B] = new MapAnimation(this)(f)
	
	
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
	  * Merges this animation with another animation
	  * @param other Another animation
	  * @param f A merging function
	  * @tparam B Type of the other animation's result
	  * @tparam R Type of merge result
	  * @return A merged animation
	  */
	def mergeWith[B, R](other: AnimationLike[B, Any])(f: (A, B) => R): Animation[R] =
		new MergeAnimation[A, B, R](this, other)(f)
	
	/**
	  * Appends another animation to this one
	  * @param another Another animation
	  * @tparam B Type of the new animation
	  * @return A new animation that first plays this animation and then the other
	  */
	def appendWith[B >: A](another: AnimationLike[B, Any], switchAt: Double = 0.5) =
		CombinedAnimation(this, another, switchAt)
}
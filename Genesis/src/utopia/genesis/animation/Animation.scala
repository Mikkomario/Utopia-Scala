package utopia.genesis.animation

import utopia.genesis.animation.Animation.{MapAnimation, RepeatingAnimation, ReverseAnimation}
import utopia.genesis.animation.transform.{AnimatedTransform, AnimationWithTransform, TimedAnimationWithTranform, TimedTransform}
import utopia.genesis.shape.path.{ProjectilePath, SPath}
import utopia.genesis.shape.shape2D.Point

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
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return A version of this animation that first progresses faster and then slows down as it nears progress 1.0
	  */
	def projectileCurved = CurvedAnimation(this, ProjectilePath())
	
	/**
	  * @return A vesion of this animation that progresses fastest at 50% progress and slowest around 0% and
	  *         100% progress
	  */
	def sPathCurved = curved(SPath.default)
	
	/**
	  * @return A version of this animation that progresses fastest at 50% progress and slowest around 0% and
	  *         100% progress
	  */
	def smoothSPathCurved = curved(SPath.smooth)
	
	/**
	  * @return A version of this animation that progresses fastest at 50% progress and slowest around 0% and
	  *         100% progress
	  */
	def verySmoothSPathCurved = curved(SPath.verySmooth)
	
	/**
	  * @return A copy of this animation that has uses reversed progress
	  */
	def reversed: Animation[A] = new ReverseAnimation[A](this)
	
	/**
	  * @return An animation that first plays this animation, and then the reverse version of this animation
	  */
	def withReverseAppended: Animation[A] = appendWith(reversed)
	
	
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
	
	/**
	  * @param curvature A curve animation used for transforming progress% values
	  * @return A curved version of this animation
	  */
	def curved(curvature: Animation[Double]) = CurvedAnimation(this, curvature)
	
	/**
	  * @param points Progress mapping points where x represents the original progress and y the mapped progress
	  * @return A new animation that curves its progress to map through the specified control points
	  */
	def curvedWith(points: Seq[Point]) = CurvedAnimation(this, points)
	
	/**
	  * @param control1 First progress control point
	  * @param control2 Second progress control point
	  * @return A curved version of this animation where the progress curve follows a cubic bezier determined by
	  *         (0, 0), control1, control2 and (1, 1) where x represents the original progress and y represents mapped
	  *         (new animation) progress
	  */
	def cubicBezierCurved(control1: Double, control2: Double) =
		CurvedAnimation(this, control1, control2)
	
	/**
	  * Appends another animation to this one
	  * @param another Another animation
	  * @param switchAt The progress point at which the animations are switched ]0, 1[ (default = 0.5)
	  * @tparam B Type of the new animation
	  * @return A new animation that first plays this animation and then the other
	  */
	def appendWith[B >: A](another: Animation[B], switchAt: Double = 0.5) =
		CombinedAnimation(this, another, switchAt)
}

object Animation
{
	// OTHER	---------------------------
	
	/**
	  * @param f An animation function. Takes animation progress (usually between 0 and 1) and returns animation state
	  * @tparam A Type of animation result
	  * @return A new animation
	  */
	def apply[A](f: Double => A): Animation[A] = new FunctionAnimation[A](f)
	
	
	// NESTED	---------------------------
	
	private class MapAnimation[A, +B](original: Animation[A])(f: A => B) extends Animation[B]
	{
		override def apply(progress: Double) = f(original(progress))
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
}
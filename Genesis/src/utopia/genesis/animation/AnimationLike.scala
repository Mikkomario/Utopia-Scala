package utopia.genesis.animation

import utopia.genesis.shape.path.{BezierFunction, CubicBezier, ProjectilePath, SPath}
import utopia.genesis.shape.shape2D.Point

object AnimationLike
{
	// TYPES    ---------------------------
	
	/**
	  * A generic animation type used when the specific type of the animation doesn't matter
	  */
	type AnyAnimation[X] = AnimationLike[X, Any]
}

/**
  * Animations are items that have different states based on progress
  * @author Mikko Hilpinen
  * @since 11.8.2019, v2.1+
  */
trait AnimationLike[+A, +Repr[+X]]
{
	// ABSTRACT	---------------------------
	
	/**
	  * @return A copy of this animation that has uses reversed progress
	  */
	def reversed: Repr[A]
	
	/**
	  * Finds a state of this animation
	  * @param progress Progress over this animation
	  * @return The state of this animation at the specified point
	  */
	def apply(progress: Double): A
	
	/**
	  * @param curvature A curve animation used for transforming progress% values
	  * @return A curved version of this animation
	  */
	def curved(curvature: AnimationLike[Double, Any]): Repr[A]
	
	/**
	  * Maps this animation
	  * @param f A mapping function. Please note that this function will be called multiple times.
	  * @tparam B Type of map result
	  * @return An animation that always provides the mapped value
	  */
	def map[B](f: A => B): Repr[B]
	
	/**
	  * Creates a new animation that repeats this one a number of times
	  * @param times The number of times this animation is repeated
	  * @return A new animation
	  */
	def repeated(times: Int): Repr[A]
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return A version of this animation that first progresses faster and then slows down as it nears progress 1.0
	  */
	def projectileCurved = curved(ProjectilePath())
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
	
	
	// OTHER	--------------------------
	
	/**
	  * @param points Progress mapping points where x represents the original progress and y the mapped progress
	  * @return A new animation that curves its progress to map through the specified control points
	  */
	def curvedWith(points: Seq[Point]) = curved(BezierFunction(points))
	/**
	  * @param control1 First progress control point
	  * @param control2 Second progress control point
	  * @return A curved version of this animation where the progress curve follows a cubic bezier determined by
	  *         (0, 0), control1, control2 and (1, 1) where x represents the original progress and y represents mapped
	  *         (new animation) progress
	  */
	def cubicBezierCurved(control1: Double, control2: Double) =
	{
		val bezier = CubicBezier(0.0, control1, control2, 1.0)
		curved(Animation { bezier(_) })
	}
}


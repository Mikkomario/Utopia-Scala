package utopia.paradigm.animation

import utopia.paradigm.path.{BezierFunction, CubicBezier}
import utopia.paradigm.shape.shape2d.vector.point.Point

object CurvedAnimation
{
	/**
	  * Creates a curved animation
	  * @param original Original animation
	  * @param control1 First control point
	  * @param control2 Second control point
	  * @tparam A Type of animation result
	  * @return A new curved animation
	  */
	def apply[A](original: Animation[A], control1: Double, control2: Double) =
	{
		val bezier = CubicBezier(0.0, control1, control2, 1.0)
		new CurvedAnimation(original, Animation { bezier(_) })
	}
	
	/**
	  * Creates a curved animation that traverses through the specified points
	  * @param original Original animation
	  * @param path Points along which the curve will traverse (generally you want points between (0,0) and (1,1)).
	  *             The y-axis represents mapped progress.
	  * @tparam A Type of animation result
	  * @return A new curved animation
	  */
	def apply[A](original: Animation[A], path: Seq[Point]) = new CurvedAnimation(original, BezierFunction(path))
}

/**
  * This transformation "curves" the original animation by altering progress
  * @author Mikko Hilpinen
  * @since Genesis 11.8.2019, v2.1+
  * @param original The original animation that is curved
  * @param curve The curve that is applied to animation progress. Most of the time you want the curve to start from
  *              0 and to end at 1
  */
case class CurvedAnimation[+A](original: AnimationLike[A, Any], curve: AnimationLike[Double, Any]) extends Animation[A]
{
	override def apply(progress: Double) = original(curve(progress))
}

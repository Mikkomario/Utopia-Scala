package utopia.genesis.animation

import utopia.genesis.util.Arithmetic._
import utopia.genesis.shape.path.{BezierPath, CubicBezier, Path}
import utopia.genesis.shape.shape2D.Point

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
	def apply[A](original: Animation[A], control1: Double, control2: Double) = new CurvedAnimation(original,
		CubicBezier[ArithMeticDouble](0.0, control1, control2, 1.0))
	
	/**
	  * Creates a curved animation that traverses through the specified points
	  * @param original Original animation
	  * @param path Points along which the curve will traverse (generally you want points between (0,0) and (1,1)).
	  *             The y-axis represents mapped progress.
	  * @tparam A Type of animation result
	  * @return A new curved animation
	  */
	def apply[A](original: Animation[A], path: Seq[Point]) = new CurvedAnimation(original,
		BezierPath[Point](path).map { p: Point => p.y })
}

/**
  * This transformation "curves" the original animation by altering progress
  * @author Mikko Hilpinen
  * @since 11.8.2019, v2.1+
  * @param original The original animation that is curved
  * @param curve The curve that is applied to animation progress. Most of the time you want the curve to start from
  *              0 and to end at 1
  */
case class CurvedAnimation[+A](original: Animation[A], curve: Path[ArithMeticDouble]) extends Animation[A]
{
	override def apply(progress: Double) = original(curve(progress))
}

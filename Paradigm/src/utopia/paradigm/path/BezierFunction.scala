package utopia.paradigm.path

import utopia.paradigm.animation.Animation
import utopia.paradigm.shape.shape2d.vector.point.Point

/**
 * Used for creating curved x-y mapping functions
 * @author Mikko Hilpinen
 * @since Genesis 9.5.2020, v2.3
 */
object BezierFunction
{
	// OTHER    ------------------------------
	
	/**
	 * Creates a curved function based on specified points
	 * @param points Points along the function (must not be empty)
	 * @return Function that traverses through each of the specified points (returns y for x). The function might
	 *         not work properly outside of the minimum and maximum x-range specified by these points (Eg. for 100 x
	 *         if you only have control points between 0 and 10 x).
	 */
	def apply(points: Seq[Point]): Animation[Double] = {
		val functionPoints = points.sortBy { _.x }
		val path = BezierPath.parts(functionPoints)
		
		FunctionPath(path, functionPoints.head.x, functionPoints.last.x)
	}
	
	
	// NESTED   ------------------------------
	
	private case class FunctionPath(paths: Seq[Path[Point]], startX: Double, endX: Double) extends Animation[Double]
	{
		override def apply(x: Double) = {
			// Finds the correct path sequence
			val p = {
				if (x < startX)
					paths.head
				else if (x > endX)
					paths.last
				else
					paths.find { p => p.start.x <= x && p.end.x >= x }.get
			}
			// Determines the progress on that path sequence
			val progress = (x - p.start.x) / (p.end.x - p.start.x)
			
			p(progress).y
		}
	}
}

package utopia.genesis.shape.path

import utopia.genesis.shape.shape2D.Point
import utopia.genesis.util.{Arithmetic, Distance}

/**
 * Used for creating curved x-y mapping functions
 * @author Mikko Hilpinen
 * @since 9.5.2020, v2.3
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
	def apply(points: Seq[Point]): Double => Double =
	{
		val functionPoints = points.sortBy { _.x }.map { p => FunctionPoint(p.x, p.y) }
		val path = BezierPath(functionPoints, sequencesPerPart = 0)
		FunctionPath(path, functionPoints.head.x, functionPoints.last.x).apply
	}
	
	
	// NESTED   ------------------------------
	
	private case class FunctionPath(path: Path[FunctionPoint], startX: Double, endX: Double)
	{
		private val length = endX - startX
		
		def apply(x: Double) =
		{
			val pathPosition = (x - startX) / length
			path(pathPosition).y
		}
	}
	
	private case class FunctionPoint(x: Double, y: Double) extends Arithmetic[FunctionPoint, FunctionPoint] with Distance
	{
		override def -(another: FunctionPoint) = FunctionPoint(x - another.x, y - another.y)
		
		override def repr = this
		
		override def *(mod: Double) = FunctionPoint(x * mod, y * mod)
		
		override def +(another: FunctionPoint) = FunctionPoint(x + another.x, y + another.y)
		
		override def length = x
	}
}

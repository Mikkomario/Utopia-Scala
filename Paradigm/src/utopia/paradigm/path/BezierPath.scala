package utopia.paradigm.path

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.HasLength
import utopia.flow.operator.combine.{Combinable, LinearScalable}
import utopia.flow.util.RangeExtensions._

/**
  * This class calculates a smooth path between points that consists of bezier curves
  * @author Mikko Hilpinen
  * @since Genesis 22.6.2019, v2.1+
  */
object BezierPath
{
	/**
	  * Calculates a bezier path between the specified points
	  * @param points The points that form the path. Must be non-empty.
	  * @param sequencesPerPart The number of sequences used for standardizing velocity or t within each part
	  * @tparam P The type of path point
	  * @return A bezier path between paths
	  * @throws IllegalArgumentException If points is empty
	  */
	def apply[P <: Combinable[P, P] with LinearScalable[P] with HasLength](points: Seq[P], sequencesPerPart: Int = 6) =
	{
		if (points.isEmpty)
			throw new IllegalArgumentException("Bezier path must be initialized with at least 1 point")
		else if (points hasSize 1)
			EmptyPath(points.head)
		else if (points hasSize 2)
			LinearPath(points.head, points(1))
		else {
			val paths = calculatePath(points)
			if (sequencesPerPart > 0)
				CompoundPath(paths.map { _.withStandardizedVelocity(sequencesPerPart) })
			else
				CompoundPath(paths)
		}
	}
	
	/**
	 * Calculates a bezier path between the specified points
	 * @param points The points that form the path. Must be at least 3 items.
	 * @tparam P The type of path point
	 * @return Sequence of paths between speified points
	 * @throws IllegalArgumentException If there are less than 3 points
	 */
	def parts[P <: Combinable[P, P] with LinearScalable[P] with HasLength](points: Seq[P]) =
	{
		if (points.size < 3)
			throw new IllegalArgumentException("Cubic Bezier path must be initialized with at least 3 points")
		else
			calculatePath(points)
	}
	
	private def calculatePath[P <: Combinable[P, P] with LinearScalable[P] with HasLength](points: Seq[P]) =
	{
		// Number of curves
		val n = points.size - 1
		
		// Base target consists of three parts: Beginning (p0 + 2p1), middle ((2pn + pn+1)*2) and end (8pn-1 + pn)
		val baseTarget = (points.head + points(1) * 2) +:
			(1 until n - 1).map { i => (points(i) * 2 + points(i + 1)) * 2 } :+ points(n - 1) * 8 + points(n)
		
		// Lower diagonal has length n-1 and consists of 1's followed by a 2
		val lowerDiagonal = Vector.fill(n - 2)(1.0) :+ 2.0
		
		// Main diagonal consists of 2 followed by 4s and ending with 7. Length = n
		val mainDiagonal = 2.0 +: Vector.fill(n - 2)(4.0) :+ 7.0
		
		// Basic upper diagonal only consists of 1's. Length = n-1
		val baseUpperDiagonal = Vector.fill(n - 1)(1.0)
		
		// Calculates the real upper diagonal. Each value requires the previous one.
		val firstUpperDiagonal = baseUpperDiagonal.head / mainDiagonal.head
		val upperDiagonal: Vector[Double] = (1 until (n - 1)).foldMapToVector(firstUpperDiagonal) { (last, i) =>
			baseUpperDiagonal(i) / (mainDiagonal(i) - lowerDiagonal(i - 1) * last) }
		
		// Calculates the real target. Again, each value is dependent from the one before
		val firstTarget = baseTarget.head / mainDiagonal.head
		val target: Vector[P] = (1 until n).foldMapToVector(firstTarget) { (last, i) =>
			val scale = 1 / (mainDiagonal(i) - lowerDiagonal(i - 1) * upperDiagonal(i - 1))
			(baseTarget(i) - last * lowerDiagonal(i - 1)) * scale
		}
		
		// Calculates the first control points from end to beginning
		val lastControl1 = target.last
		val controlPoints1: Vector[P] = ((n - 2) to 0 by -1).foldMapToVector(lastControl1) { (next, i) =>
			target(i) - next * upperDiagonal(i) }.reverse
		
		// Calculates the second control points from the first (last one calculated separately)
		val lastControl2 = (points.last + lastControl1) / 2
		val controlPoints2 = (0 until (n - 1)).map { i => points(i + 1) * 2 - controlPoints1(i + 1) } :+ lastControl2
		
		// Creates the curves
		(0 until n).map { i => CubicBezier(points(i), controlPoints1(i), controlPoints2(i), points(i + 1)) }
	}
}

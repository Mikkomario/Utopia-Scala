package utopia.genesis.test

import utopia.paradigm.path.BezierFunction
import utopia.paradigm.shape.shape2d.vector.point.Point

/**
 * Tests bezier function
 * @author Mikko Hilpinen
 * @since 9.5.2020, v2.3
 */
object BezierFunctionTest extends App
{
	private val points = Vector(Point(0, 23), Point(20, 20), Point(100, 10), Point(200, 5), Point(1000, 1))
	private val function = BezierFunction(points)
	
	(0 to (points.size * 7)).foreach { progress =>
		val x = progress * 10
		val y = math.round(function(x) * 10) / 10.0
		println(s"$x \t-> ${"-" * (y * 3).toInt} ($y)")
	}
}

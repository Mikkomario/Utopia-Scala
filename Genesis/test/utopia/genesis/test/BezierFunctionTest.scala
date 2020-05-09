package utopia.genesis.test

import utopia.genesis.shape.path.BezierFunction
import utopia.genesis.shape.shape2D.Point

/**
 * Tests bezier function
 * @author Mikko Hilpinen
 * @since 9.5.2020, v2.3
 */
object BezierFunctionTest extends App
{
	private val points = Vector(Point(0, 0), Point(1, 1), Point(2, 3), Point(3, 6), Point(4, 10), Point(5, 0), Point(6, 20), Point(7, 25))
	private val function = BezierFunction(points)
	
	(0 to 30).foreach { progress =>
		val x = progress / 4.0
		val y = math.round(function(progress / 4.0) * 10) / 10.0
		println(s"$x \t-> ${"-" * (y * 2).toInt} ($y)")
	}
}

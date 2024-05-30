package utopia.paradigm.test

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.point.Point

/**
  * Tests ProjectionPath functions on a Line
  * @author Mikko Hilpinen
  * @since 29.05.2024, v1.6
  */
object LineProjectionTest extends App
{
	// Line from (1,1) to (3,3)
	val l1 = Line(Pair(1.0, 3.0).map(Point.twice))
	
	assert(l1.forDt(0) ~== Point.twice(1))
	assert(l1.forDt(l1.length) ~== Point.twice(3))
	assert(l1.forDt(l1.length / 2) ~== Point.twice(2))
	
	// A horizontal line from x 1 to 3
	val l2 = Line(Pair(1.0, 3.0).map { X(_).toPoint })
	
	assert(l2.tAxis.y ~== 0.0)
	assert(l2.t0 ~== 1.0)
	assert(l2.tLength ~== 2.0)
	assert(l2.dtFor(Point.origin) ~== -1.0)
	assert(l2.matching(Point(5, 8)) ~== Point(5, 0))
	assert(l2.liftMatching(Point(5.8)).isEmpty)
	
	println("Done!")
}

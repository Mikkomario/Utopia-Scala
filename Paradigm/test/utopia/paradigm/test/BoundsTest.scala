package utopia.paradigm.test

import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
  * Tests some Bounds functions
  * @author Mikko Hilpinen
  * @since 15.11.2022, v1.2
  */
object BoundsTest extends App
{
	ParadigmDataType.setup()
	
	// Tests Bounds construction
	val p = Point(720.0, 500.0)
	val s = Size(100.0, 80.0)
	val b = Bounds(p, s)
	
	println(b)
	assert(b.position == p)
	assert(b.size == s)
	
	// Tests relativize
	val p2 = Point(740.0, 520.0)
	
	assert(b.relativize(p2) ~== Point(0.2, 0.25))
	assert(b.relativeToAbsolute(Point(0.2, 0.25)) ~== p2)
	
	println("Success!")
}

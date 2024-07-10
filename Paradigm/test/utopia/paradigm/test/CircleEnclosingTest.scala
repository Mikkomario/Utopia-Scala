package utopia.paradigm.test

import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point

/**
  *
  * @author Mikko Hilpinen
  * @since 10.07.2024, v
  */
object CircleEnclosingTest extends App
{
	ParadigmDataType.setup()
	
	val points = Vector(
		Vector2D(72.17765067804277, -29.308642349143337),
		Vector2D(72.72699864370769, -29.360885903824567),
		Vector2D(72.17765067804277, -29.308642349143337),
		Vector2D(73.0197130320455, -29.33726344038675),
		Vector2D(72.72699864370769, -29.360885903824567),
		Vector2D(73.23782200236872, -29.093332364647985)
	)
	
	val circle = Circle.enclosing(points)
	println(circle)
	
	/*
	points.foreach { p => assert(circle.contains(p), s"$p in $circle with distance of ${
		p.distanceFrom(circle.origin) } (${ p.distanceFrom(circle.origin) - circle.radius } too much)") }
	*/
	assert(circle == Circle(Point(72.70773634020574, -29.20098735689566), 0.5409070221272845))
}

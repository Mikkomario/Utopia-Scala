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
	assert(circle == Circle(Point(72.70773634020574, -29.20098735689566), 0.5409070221272845))
	
	val circles = Vector(
		Circle(Point(2.2511032090371677, -39.77201557248378), 0.0),
		Circle(Point(0.42326767965418294, -40.87306457256923), 0.0)
	)
	
	val enclosing2 = Circle.enclosingCircles(circles)
	
	println(Circle.weighedCentroidOf(circles))
	println(enclosing2)
}

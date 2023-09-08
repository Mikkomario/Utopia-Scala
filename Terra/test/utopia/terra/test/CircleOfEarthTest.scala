package utopia.terra.test

import utopia.flow.operator.EqualsExtensions._
import utopia.terra.controller.coordinate.world.CircleOfEarth
import utopia.terra.model.angular.LatLong
import utopia.terra.model.enumeration.CompassDirection.North
import utopia.terra.model.world.circle.CircleSurfacePoint

/**
  * Tests location mapping on the circle of earth
  * @author Mikko Hilpinen
  * @since 8.9.2023, v1.0
  */
object CircleOfEarthTest extends App
{
	val p1 = CircleSurfacePoint(LatLong.origin)
	
	assert(p1.vector.x ~== CircleOfEarth.equatorVectorRadius)
	assert(p1.vector.y ~== 0.0)
	
	val p2 = CircleSurfacePoint(LatLong.fromDegrees(45.0, 0.0))
	
	assert(p2.latLong.latitude ~== North.degrees(45.0))
	assert(p2.vector.x ~== CircleOfEarth.equatorVectorRadius / 2.0, p2.vector.x)
	assert(p2.vector.y ~== 0.0)
	
	val p3 = CircleSurfacePoint(LatLong.fromDegrees(0.0, 90.0))
	
	assert(p3.vector.x ~== 0.0)
	assert(p3.vector.y ~== -CircleOfEarth.equatorVectorRadius)
	
	val p4 = CircleSurfacePoint.northPole
	
	assert(p4.latLong.latitude ~== North.degrees(90.0))
	
	println("Success!")
}

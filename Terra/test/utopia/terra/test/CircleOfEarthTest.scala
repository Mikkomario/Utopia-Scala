package utopia.terra.test

import utopia.flow.operator.EqualsExtensions._
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.terra.controller.coordinate.world.CircleOfEarth
import utopia.terra.model.angular.LatLong
import utopia.terra.model.enumeration.CompassDirection.{East, North, South, West}
import utopia.terra.model.world.circle.CircleSurfacePoint

/**
  * Tests location mapping on the circle of earth
  * @author Mikko Hilpinen
  * @since 8.9.2023, v1.0
  */
object CircleOfEarthTest extends App
{
	import CircleOfEarth.latLongToVector
	import CircleOfEarth.equatorVectorRadius
	
	assert(latLongToVector(LatLong.fromDegrees(0.0, 0.0)) ~==
		Vector2D(equatorVectorRadius, 0.0))
	assert(latLongToVector(LatLong.fromDegrees(22.5, 0.0)) ~==
		Vector2D(equatorVectorRadius * 0.75, 0.0))
	assert(latLongToVector(LatLong.fromDegrees(45.0, 0.0)) ~==
		Vector2D(equatorVectorRadius * 0.5, 0.0))
	assert(latLongToVector(LatLong.fromDegrees(67.5, 0.0)) ~==
		Vector2D(equatorVectorRadius * 0.25, 0.0))
	assert(latLongToVector(LatLong.fromDegrees(90.0, 0.0)) ~==
		Vector2D(0.0, 0.0))
	
	val p1 = CircleSurfacePoint(LatLong.origin)
	
	assert(p1.vector.x ~== CircleOfEarth.equatorVectorRadius, p1)
	assert(p1.vector.y ~== 0.0)
	
	val p2 = CircleSurfacePoint(LatLong.fromDegrees(45.0, 0.0))
	
	assert(p2.latLong.latitude ~== North.degrees(45.0))
	assert(p2.vector.x ~== CircleOfEarth.equatorVectorRadius / 2.0, p2)
	assert(p2.vector.y ~== 0.0)
	
	val p3 = CircleSurfacePoint(LatLong.fromDegrees(0.0, 90.0))
	
	assert(p3.vector.x ~== 0.0)
	assert(p3.vector.y ~== -CircleOfEarth.equatorVectorRadius)
	
	val p4 = CircleSurfacePoint.northPole
	
	assert(p4.latLong.latitude ~== North.degrees(90.0))
	
	val helsinki = CircleSurfacePoint(LatLong.fromDegrees(60.192059, 24.945831))
	val lahti = CircleSurfacePoint(LatLong.fromDegrees(60.9827, 25.6615))
	
	println(s"Distance from Helsinki to Lahti is ${ lahti.linearDistanceFrom(helsinki) }")
	
	val cloncurry = CircleSurfacePoint(LatLong.fromDegrees(-20.7110, 140.5050))
	val richmond = CircleSurfacePoint(LatLong.fromDegrees(-20.7375, 143.1290))
	
	println(s"Distance from Cloncurry to Richmond is ${ cloncurry.linearDistanceFrom(richmond) }")
	
	val north = (lahti + North.degrees(0.00001)).vector - lahti.vector
	val south = (lahti + South.degrees(0.00001)).vector - lahti.vector
	val east = (lahti + East.degrees(0.00001)).vector - lahti.vector
	val west = (lahti + West.degrees(0.00001)).vector - lahti.vector
	
	println(north.direction.degrees)
	println(east.direction.degrees)
	println(south.direction.degrees)
	println(west.direction.degrees)
	/*
	assert((north.direction - south.direction).absolute.degrees ~== 180.0)
	assert((east.direction - west.direction).absolute.degrees ~== 180.0)
	assert((north.direction - east.direction).absolute.degrees ~== 90.0)
	assert((north.direction - west.direction).absolute.degrees ~== 90.0)
	assert((south.direction - east.direction).absolute.degrees ~== 90.0)
	assert((north.direction - west.direction).absolute.degrees ~== 90.0)
	*/
	println("Success!")
}

package utopia.terra.test

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.sign.Sign.Negative
import utopia.paradigm.angular.Angle
import utopia.terra.model.angular.LatLong
import utopia.terra.model.enumeration.CompassDirection.{East, North, South, West}

/**
  * Tests conversion from double numbers to latitude longitude pairs
  * @author Mikko Hilpinen
  * @since 5.9.2023, v1.0
  */
object LatLongTest extends App
{
	val ll0 = LatLong.degrees(0.0, 0.0)
	val ll1 = LatLong.degrees(60.8, 15.3)
	val ll2 = LatLong.degrees(-30, 15.3)
	val ll3 = LatLong.degrees(60.8, 120.3)
	val ll4 = LatLong.degrees(60.8, -83.2)
	
	assert(ll0.latitude.isZero, ll0.latitude)
	assert(ll1.latitude.sign.isNegative)
	assert(ll1.latitude == North.degrees(60.8))
	assert(ll2.latitude == South.degrees(30))
	
	assert(ll0.longitude == Angle.zero)
	assert(East.sign == Negative)
	assert(ll1.longitude == Angle.degrees(344.7))
	assert(ll3.longitude == Angle.degrees(239.7))
	assert(ll4.longitude == Angle.degrees(83.2), ll4.longitude)
	
	assert(ll0.latitudeDegrees == 0.0)
	assert(ll1.latitudeDegrees ~== 60.8, ll1.latitudeDegrees)
	assert(ll2.latitudeDegrees ~== -30.0, ll2.latitudeDegrees)
	
	assert(ll0.longitudeDegrees == 0.0)
	assert(ll1.longitudeDegrees ~== 15.3)
	assert(ll3.longitudeDegrees ~== 120.3)
	assert(ll4.longitudeDegrees ~== -83.2)
	
	// Tests directions
	assert(ll1.northSouthSide == North, ll1)
	assert(ll1.eastWestSide == East)
	assert(ll2.northSouthSide == South)
	assert(ll2.eastWestSide == East)
	assert(ll3.northSouthSide == North)
	assert(ll3.eastWestSide == East)
	assert(ll4.eastWestSide == West)
	
	println("Done!")
}

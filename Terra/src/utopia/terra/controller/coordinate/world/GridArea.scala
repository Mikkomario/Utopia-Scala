package utopia.terra.controller.coordinate.world

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.terra.controller.coordinate.GlobeMath
import utopia.terra.model.angular.{LatLong, LatLongRotation, NorthSouthRotation}
import utopia.terra.model.enumeration.CompassDirection.{EastWest, NorthSouth, South}
import utopia.terra.model.world.WorldDistance
import utopia.terra.model.world.grid.{AerialGridPoint, GridSurfacePoint}

object GridArea extends VectorDistanceConversion
{
	/**
	  * The vector length distance covered by one degree of travel along the latitude circle (southward)
	  */
	val oneDegreeLatitudeArcVectorLength = 10000.0
	/**
	  * The assumed circumference of the earth in vector distance units
	  */
	val globeVectorCircumference = oneDegreeLatitudeArcVectorLength * 360.0
	/**
	  * The assumed (mean) radius of the earth sphere in vector distance units
	  */
	val globeVectorRadius = globeVectorCircumference / (math.Pi * 2.0)
	
	
	// Unit distance matches 0.001 degrees of travel along the north-to-south axis
	// I.e. 100 units = 1 degree of travel
	override val unitDistance: Distance = GlobeMath.meanCircumference / 360.0 / oneDegreeLatitudeArcVectorLength
}

/**
  * Represent a section of the earth,
  * that has been flattened to a 2D plane.
  * The X axis matches the latitude "line" (situational)
  * and the Y axis matches the longitude "line" (also situational).
  * @author Mikko Hilpinen
  * @since 2.11.2023, v1.0.1
  * @param origin The location of the "origin" of this grid area, matching the (0,0) vector coordinate.
  *               Please note that locations far from the origin will be more inaccurate.
  */
class GridArea(val origin: LatLong) extends FlatWorldView[GridSurfacePoint, AerialGridPoint]
{
	// ATTRIBUTES   ------------------
	
	import GridArea._
	
	// Unit distance matches 0.001 degrees of travel along the north-to-south axis
	// I.e. 100 units = 1 degree of travel
	override val unitDistance: Distance = GridArea.unitDistance
	
	// Radius of the east-to-west circle at the origin latitude level, in vector space
	private val eastWestRadiusAtOrigin = GlobeMath.eastWestRadiusAtLatitude(origin.latitude, globeVectorRadius).first
	
	protected override implicit def worldView: GridArea = this
	
	
	// IMPLEMENTED  ------------------
	
	override def apply(latLong: LatLong) = GridSurfacePoint(latLong)
	override def apply(latLong: LatLong, altitude: WorldDistance) = AerialGridPoint(latLong, altitude)
	
	override def aerialVector(vector: Vector3D): AerialGridPoint = AerialGridPoint(vector)
	override def surfaceVector(vector: DoubleVector): GridSurfacePoint = GridSurfacePoint(vector.toVector2D)
	
	/**
	  * Converts a latitude-longitude coordinate into a grid-based vector
	  * @param latLong A latitude longitude coordinate
	  * @return A vector that matches that coordinate in this system
	  */
	override def latLongToVector(latLong: LatLong) = {
		// Determines the angular position vector relative to the origin
		val position = latLong - origin
		val x = latitudeRotationToVectorLength(position.northSouth)
		val eastWestArcLength = position.eastWest.arcLengthOver(eastWestRadiusAtOrigin)
		// "Simulated" value based on the radius difference at the origin latitude level
		val latitudeRotationOfEastWestTravel = NorthSouth.rotation.forArcLength(eastWestArcLength, globeVectorRadius)
		val y = latitudeRotationToVectorLength(latitudeRotationOfEastWestTravel)
		
		Vector2D(x, y)
	}
	/**
	  * Converts a grid-based vector into a latitude-longitude coordinate
	  * @param vector A vector in this grid-based system
	  * @return A latitude-longitude coordinate that matches that vector
	  */
	override def vectorToLatLong(vector: DoubleVector) = {
		// Converts vector length (X) to latitude angular travel
		val northSouthPosition = vectorLengthToLatitudeRotation(vector.x)
		// Converts vector length (Y) to latitude angular travel first
		// and then corrects for the difference in radii between the latitude and longitude circles
		val simulatedLatitudeRotationOfY = vectorLengthToLatitudeRotation(vector.y)
		val eastWestArcLength = simulatedLatitudeRotationOfY.arcLengthOver(globeVectorRadius)
		val eastWestPosition = EastWest.rotation.forArcLength(eastWestArcLength, eastWestRadiusAtOrigin)
		
		// Converts from relative-to-origin space to relative to (0,0) lat long -space
		origin + LatLongRotation(northSouthPosition, eastWestPosition)
	}
	
	
	// OTHER    ---------------------
	
	private def latitudeRotationToVectorLength(latitudeRotation: NorthSouthRotation) =
		latitudeRotation.south.degrees * oneDegreeLatitudeArcVectorLength
	private def vectorLengthToLatitudeRotation(vectorLength: Double) =
		South.degrees(vectorLength / oneDegreeLatitudeArcVectorLength)
}

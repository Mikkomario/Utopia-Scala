package utopia.terra.controller.coordinate.world

import utopia.paradigm.angular.Rotation
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.GlobeMath
import utopia.terra.model.angular.{LatLong, LatLongRotation}
import utopia.terra.model.world.grid.{GridPoint, GridSurfacePoint}

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
class GridArea(origin: LatLong) extends WorldView[Vector2D, Vector3D, GridSurfacePoint, GridPoint]
{
	// ATTRIBUTES   ------------------
	
	private val globeVectorRadius = 10000.0
	// Length of one degree of latitude change, in vector space
	private val oneLatitudeDegreeArcLength =
		Rotation.ofDegrees(1.0).arcLengthOver(globeVectorRadius)
	
	// Unit distance matches 0.001 degrees of travel along the north-to-south axis
	// I.e. 100 units = 1 degree of travel
	override val unitDistance: Distance = GlobeMath.meanRadius / globeVectorRadius / 3.60
	
	// Radius of the east-to-west circle at the origin latitude level, in vector space
	private val eastWestRadiusAtOrigin = GlobeMath
		.eastWestRadiusAtLatitude(origin.latitude, globeVectorRadius).first
	
	private implicit def me: GridArea = this
	
	
	// IMPLEMENTED  ------------------
	
	override def apply(latLong: LatLong) = GridSurfacePoint(latLong)
	override def apply(latLong: LatLong, altitude: Distance) = GridPoint(latLong, altitude)
	
	override def aerialVector(vector: Vector3D): GridPoint = GridPoint(vector)
	override def surfaceVector(vector: Vector2D): GridSurfacePoint = GridSurfacePoint(vector)
	
	
	// OTHER    ---------------------
	
	/**
	  * Converts a latitude-longitude coordinate into a grid-based vector
	  * @param latLong A latitude longitude coordinate
	  * @return A vector that matches that coordinate in this system
	  */
	def latLongToVector(latLong: LatLong) = {
		// Determines the angular position vector relative to the origin
		val position = latLong - origin
		val x = latitudeRotationToVectorLength(position.northSouth)
		val eastWestArcLength = position.eastWest.arcLengthOver(eastWestRadiusAtOrigin)
		// "Simulated" value based on the radius difference at the origin latitude level
		val latitudeRotationOfEastWestTravel = Rotation.forArcLength(eastWestArcLength, globeVectorRadius)
		val y = latitudeRotationToVectorLength(latitudeRotationOfEastWestTravel)
		
		Vector2D(x, y)
	}
	/**
	  * Converts a grid-based vector into a latitude-longitude coordinate
	  * @param vector A vector in this grid-based system
	  * @return A latitude-longitude coordinate that matches that vector
	  */
	def vectorToLatLong(vector: Vector2D) = {
		// Converts vector length (X) to latitude angular travel
		val northSouthPosition = vectorLengthToLatitudeRotation(vector.x)
		// Converts vector length (Y) to latitude angular travel first
		// and then corrects for the difference in radii between the latitude and longitude circles
		val simulatedLatitudeRotationOfX = vectorLengthToLatitudeRotation(vector.y)
		val eastWestArcLength = simulatedLatitudeRotationOfX.arcLengthOver(globeVectorRadius)
		val eastWestPosition = Rotation.forArcLength(eastWestArcLength, eastWestRadiusAtOrigin)
		
		// Converts from relative-to-origin space to relative to (0,0) lat long -space
		origin + LatLongRotation(northSouthPosition, eastWestPosition)
	}
	
	private def latitudeRotationToVectorLength(latitudeRotation: Rotation) =
		latitudeRotation.degrees / oneLatitudeDegreeArcLength * 100
	private def vectorLengthToLatitudeRotation(vectorLength: Double) =
		Rotation.ofDegrees(oneLatitudeDegreeArcLength * vectorLength / 100.0)
}

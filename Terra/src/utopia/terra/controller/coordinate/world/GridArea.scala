package utopia.terra.controller.coordinate.world

import utopia.paradigm.angular.Rotation
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.GlobeMath
import utopia.terra.model.angular.LatLong

/**
  * Represent a section of the earth,
  * that has been flattened to a 2D plane.
  * The X axis matches the longitude "line" (situational)
  * and the Y axis matches the latitude "line" (also situational).
  * @author Mikko Hilpinen
  * @since 2.11.2023, v1.0.1
  * @param origin The location of the "origin" of this grid area, matching the (0,0) vector coordinate.
  *               Please note that locations far from the origin will be more inaccurate.
  */
class GridArea(origin: LatLong) extends WorldView[Vector2D, Vector3D, Vector2D, Vector3D]
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
	
	
	// IMPLEMENTED  ------------------
	
	override def apply(latLong: LatLong): Vector2D = {
		// Determines the angular position vector relative to the origin
		val position = latLong - origin
		val y = latitudeRotationToVectorLength(position.northSouth)
		val eastWestArcLength = position.eastWest.arcLengthOver(eastWestRadiusAtOrigin)
		// "Simulated" value based on the radius difference at the origin latitude level
		val latitudeRotationOfEastWestTravel = Rotation.forArcLength(eastWestArcLength, globeVectorRadius)
		val x = latitudeRotationToVectorLength(latitudeRotationOfEastWestTravel)
		
		Vector2D(x, y)
	}
	override def apply(latLong: LatLong, altitude: Distance): Vector3D = {
		val surfaceLocation = apply(latLong)
		val z = vectorLengthOf(altitude)
		Vector3D(surfaceLocation.z, surfaceLocation.y, z)
	}
	
	override def aerialVector(vector: Vector3D): Vector3D = vector
	override def surfaceVector(vector: Vector2D): Vector2D = vector
	
	
	// OTHER    ---------------------
	
	private def latitudeRotationToVectorLength(latitudeRotation: Rotation) =
		latitudeRotation.degrees / oneLatitudeDegreeArcLength * 100
}

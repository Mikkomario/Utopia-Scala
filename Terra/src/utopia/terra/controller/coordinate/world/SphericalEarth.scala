package utopia.terra.controller.coordinate.world

import utopia.paradigm.angular.Angle
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.GlobeMath
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.sphere.{SpherePoint, SphereSurfacePoint}

/**
 * This world view assumes that the Earth is a perfect symmetric sphere on the sea level.
 * Assumes mean radius. See [[GlobeMath.meanRadius]].
 *
 * In the vector form, (0,0,0) lies at the center of the Earth sphere.
 * The Z-vector pierces the sphere through the south and the north poles
  * (where north is positive and south is negative).
 * The X-Y plane (Z=0) covers the whole equator.
 * X-axis intersects with the equator (on the positive side) at 0 degree longitude coordinates.
 * Positive longitude moves from east to the west.
 * Length of 1.0 equals the Earth's assumed mean radius.
 *
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
object SphericalEarth extends WorldView[Vector3D, Vector3D, SphereSurfacePoint, SpherePoint]
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * The radius of the spherical globe in vector coordinate system
	  */
	val globeVectorRadius = 100000.0
	
	override val unitDistance: Distance = GlobeMath.meanRadius / globeVectorRadius
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(latLong: LatLong): SphereSurfacePoint = SphereSurfacePoint(latLong)
	override def apply(latLong: LatLong, altitude: Distance): SpherePoint = SpherePoint(latLong, altitude)
	
	override def surfaceVector(vector: Vector3D): SphereSurfacePoint = SpherePoint(vector).toSurfacePoint
	override def aerialVector(vector: Vector3D): SpherePoint = SpherePoint(vector)
	
	/**
	  * @param latLong A latitude-longitude coordinate
	  * @return A vector representing that same point on the spherical Earth's surface at the sea level.
	  */
	override def latLongToVector(latLong: LatLong) = {
		// Calculates the position on the X-Z plane based on latitude, which determines the east-west radius,
		// as well as the final Z-coordinate
		val (eastWestRadius, z) = GlobeMath.eastWestRadiusAtLatitude(latLong.latitude, globeVectorRadius).toTuple
		
		// Calculates the correct position on the east-west circle along the X-Y plane
		val xy = Vector2D.lenDir(eastWestRadius, latLong.longitude)
		
		// Combines the two positions, forming a 3D vector
		Vector3D(xy.dimensions.withZ(z))
	}
	/**
	  * @param vector A vector coordinate in the spherical Earth system
	  * @return A latitude-longitude coordinate that matches that same location
	  */
	override def vectorToLatLong(vector: Vector3D) = {
		// Calculates the correct longitude on the X-Y plane
		val longitude = vector.direction
		// Also calculates the distance from the Z-axis along the X-Y plane
		val poleDistance = vector.length
		
		// Determines the latitude based on Z and this calculated distance
		val latitude = Vector2D(poleDistance, vector.z).direction - Angle.right
		
		LatLong(latitude, longitude)
	}
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param vector A vector coordinate in the spherical Earth system
	 * @return The altitude of that point above the mean sea level
	 */
	def altitudeAt(vector: Vector3D) = GlobeMath.meanRadius * (vector.length / globeVectorRadius - 1.0)
	
	/**
	 * @param vector A vector position to modify
	 * @param raise Increase in altitude to apply
	 * @return A vector position after the altitude change
	 */
	def changeAltitude(vector: Vector3D, raise: Distance) = vector + vectorLengthOf(raise)
}

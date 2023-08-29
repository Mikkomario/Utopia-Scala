package utopia.terra.controller.coordinate

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.model.angular.LatLong
import utopia.terra.model.enumeration.CompassDirection.South
import utopia.terra.model.vector.circle.{CirclePoint, CircleSurfacePoint}

/**
 * A world view where the (real) north pole (i.e. the one under Polaris) lies at the center (0,0)
 * and the Earth is a circle originating from the north pole and expanding along the X-Y plane.
 * The positive Z axis points towards the Heaven and negative Z axis towards Gehanna.
 * The sea level is a level plane, and runs along the X-Y plane at Z=0.
 *
 * Distances on the southern side of equator constitute a major difference
 * between this model and the spherical Earth model.
 * For example, the Antarctic continent has many times longer shoreline in this world view,
 * compared to the spherical model.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
object CircleOfEarth extends WorldView[Vector2D, Vector3D, CircleSurfacePoint, CirclePoint]
{
	// COMPUTED   -------------------------
	
	/**
	 * Distance that matches vector length of 1.0
	 */
	def unitDistance = GlobeMath.earthRadiusAtEquator
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(latLong: LatLong): CircleSurfacePoint = CircleSurfacePoint(latLong)
	override def apply(latLong: LatLong, altitude: Distance): CirclePoint = CirclePoint(latLong, altitude)
	
	override def surfaceVector(vector: Vector2D): CircleSurfacePoint = CircleSurfacePoint(vector)
	override def aerialVector(vector: Vector3D): CirclePoint = CirclePoint(vector)
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param distance A distance on Earth
	 * @return A vector length matching that distance (assuming a certain Earth radius)
	 */
	def vectorLengthOf(distance: Distance) = distance / unitDistance
	/**
	 * @param length A vector system length value
	 * @return A real life distance matching that value (assuming a certain Earth radius).
	 */
	def vectorLengthToDistance(length: Double) = unitDistance * length
	
	/**
	 * @param latLong A latitude-longitude coordinate
	 * @return A 2D vector representation of that coordinate along the circular plane
	 */
	def latLongToVector(latLong: LatLong) = {
		// Distance is 0 at the north pole (-90 latitude),
		// 1.0 at the equator (0.0 latitude) and
		// 2.0 at the southern rim (90 latitude)
		val distance = (latLong(South) - South.degrees(90)).degrees / 90.0
		
		// Direction matches the longitude, because 0 angle (right) matches the longitude of 0 (Greenwich, England)
		Vector2D.lenDir(distance, latLong.longitude)
	}
	
	/**
	 * @param vector A vector relative to the circle origin along the X-Y plane
	 *               (where X axis runs from the north pole towards Greenwich, England)
	 * @return A latitude-longitude coordinate that matches that vector position
	 */
	def vectorToLatLong(vector: Vector2D) = {
		// 0.0 latitude is at the equator, which lies at length 1.0
		val latitude = South.degrees((1 - vector.length) * 90.0)
		// Longitude matches the vector direction exactly
		LatLong(latitude, vector.direction)
	}
}

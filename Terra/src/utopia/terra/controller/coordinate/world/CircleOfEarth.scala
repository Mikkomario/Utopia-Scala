package utopia.terra.controller.coordinate.world

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.GlobeMath
import utopia.terra.model.angular.LatLong
import utopia.terra.model.enumeration.CompassDirection.South
import utopia.terra.model.world.circle.{CirclePoint, CircleSurfacePoint}

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
	// ATTRIBUTES   -------------------------
	
	/**
	  * Radius of the equator circle in this world view,
	  * in vector coordinate system.
	  */
	val equatorVectorRadius = 100000.0
	
	override val unitDistance = GlobeMath.earthRadiusAtEquator / equatorVectorRadius
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(latLong: LatLong): CircleSurfacePoint = CircleSurfacePoint(latLong)
	override def apply(latLong: LatLong, altitude: Distance): CirclePoint = CirclePoint(latLong, altitude)
	
	override def surfaceVector(vector: Vector2D): CircleSurfacePoint = CircleSurfacePoint(vector)
	override def aerialVector(vector: Vector3D): CirclePoint = CirclePoint(vector)
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param latLong A latitude-longitude coordinate
	 * @return A 2D vector representation of that coordinate along the circular plane
	 */
	def latLongToVector(latLong: LatLong) = {
		// Distance is 0 at the north pole (-90 latitude),
		// 100 000 at the equator (0.0 latitude) and
		// 200 000 at the southern rim (90 latitude)
		val distance = (latLong(South) - South.degrees(90)).degrees * equatorVectorRadius / 90.0
		
		// Direction matches the longitude, because 0 angle (right) matches the longitude of 0 (Greenwich, England)
		Vector2D.lenDir(distance, latLong.longitude)
	}
	/**
	 * @param vector A vector relative to the circle origin along the X-Y plane
	 *               (where X axis runs from the north pole towards Greenwich, England)
	 * @return A latitude-longitude coordinate that matches that vector position
	 */
	def vectorToLatLong(vector: Vector2D) = {
		// 0.0 latitude is at the equator, which lies at length 100 000
		val latitude = South.degrees((1 - vector.length / equatorVectorRadius) * 90.0)
		// Longitude matches the vector direction exactly
		LatLong(latitude, vector.direction)
	}
}

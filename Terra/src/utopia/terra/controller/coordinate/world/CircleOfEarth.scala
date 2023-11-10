package utopia.terra.controller.coordinate.world

import utopia.paradigm.angular.Rotation
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.GlobeMath
import utopia.terra.model.angular.LatLong
import utopia.terra.model.enumeration.CompassDirection.South
import utopia.terra.model.world.WorldDistance
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
	/**
	  * Radius of the equator circle in this world view,
	  * in "distance"
	units
	*/
	val equatorCircleRadius = GlobeMath.earthRadiusAtEquator * math.Pi / 2.0
	
	override val unitDistance = equatorCircleRadius / equatorVectorRadius
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(latLong: LatLong): CircleSurfacePoint = CircleSurfacePoint(latLong)
	override def apply(latLong: LatLong, altitude: Distance): CirclePoint = CirclePoint(latLong, altitude)
	
	override def surfaceVector(vector: Vector2D): CircleSurfacePoint = CircleSurfacePoint(vector)
	override def aerialVector(vector: Vector3D): CirclePoint = CirclePoint(vector)
	
	/**
	  * @param latLong A latitude-longitude coordinate
	  * @return A 2D vector representation of that coordinate along the circular plane
	  */
	override def latLongToVector(latLong: LatLong) = {
		// Distance is 0 at the north pole (-90 latitude),
		// 100 000 at the equator (0.0 latitude) and
		// 200 000 at the southern rim (90 latitude)
		// V = (lat + 90 degrees) * R / 90 degrees
		// 90 degrees = Pi/2
		val vectorDistance = (latLong + South.radians(math.Pi / 2.0))(South).clockwiseRadians *
			equatorVectorRadius * 2.0 / math.Pi
		// Direction matches the longitude, because 0 angle (right) matches the longitude of 0 (Greenwich, England)
		Vector2D.lenDir(vectorDistance, latLong.longitude)
	}
	/**
	  * @param vector A vector relative to the circle origin along the X-Y plane
	  *               (where X axis runs from the north pole towards Greenwich, England)
	  * @return A latitude-longitude coordinate that matches that vector position
	  */
	override def vectorToLatLong(vector: Vector2D) = {
		// 0.0 latitude is at the equator, which lies at length 100 000
		// Lat = 90 degrees * vectorLength / R - 90 degrees
		val latitude = South.radians(math.Pi * vector.length / (equatorVectorRadius * 2.0) - (math.Pi / 2.0))
		// Longitude matches the vector direction exactly
		LatLong(latitude, vector.direction)
	}
	
	
	// OTHER    ---------------------
	
	// TODO: Continue working on this once we have the absolute rotation class
	def latitudeTravelToDistance(latitude: Rotation): WorldDistance =
		distance(equatorVectorRadius * latitude.absoluteRadians * 2.0 / math.Pi)
}

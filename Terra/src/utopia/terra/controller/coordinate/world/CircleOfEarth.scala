package utopia.terra.controller.coordinate.world

import utopia.paradigm.angular.Rotation
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.terra.controller.coordinate.GlobeMath
import utopia.terra.model.angular.{LatLong, NorthSouthRotation}
import utopia.terra.model.enumeration.CompassDirection.South
import utopia.terra.model.world.WorldDistance
import utopia.terra.model.world.circle.{AerialCirclePoint, CircleSurfacePoint}

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
object CircleOfEarth extends FlatWorldView[CircleSurfacePoint, AerialCirclePoint]
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * Radius of the equator circle in this world view,
	  * in vector coordinate system.
	  */
	private val equatorVectorRadius = 100000.0
	/**
	  * Radius of the equator circle in this world view,
	  * in "distance" units
	*/
	private val equatorCircleRadius = GlobeMath.earthRadiusAtEquator * math.Pi / 2.0
	
	override val unitDistance = equatorCircleRadius / equatorVectorRadius
	
	/**
	  * The radius of the equator
	  */
	val equatorRadius: WorldDistance = WorldDistance(equatorCircleRadius, equatorVectorRadius)(this)
	
	
	// IMPLEMENTED  -------------------------
	
	override protected implicit def worldView: VectorDistanceConversion = this
	
	override def apply(latLong: LatLong): CircleSurfacePoint = CircleSurfacePoint(latLong)
	override def apply(latLong: LatLong, altitude: WorldDistance): AerialCirclePoint =
		AerialCirclePoint(latLong, altitude)
	
	override def surfaceVector(vector: DoubleVector): CircleSurfacePoint = CircleSurfacePoint(vector.toVector2D)
	override def aerialVector(vector: Vector3D): AerialCirclePoint = AerialCirclePoint(vector)
	
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
		val vectorDistance = (latLong.south + Rotation.quarter).radians *
			equatorVectorRadius * 2.0 / math.Pi
		// Direction matches the longitude, because 0 angle (right) matches the longitude of 0 (Greenwich, England)
		Vector2D.lenDir(vectorDistance, latLong.longitude)
	}
	/**
	  * @param vector A vector relative to the circle origin along the X-Y plane
	  *               (where X axis runs from the north pole towards Greenwich, England)
	  * @return A latitude-longitude coordinate that matches that vector position
	  */
	override def vectorToLatLong(vector: DoubleVector) = {
		// 0.0 latitude is at the equator, which lies at length 100 000
		// Lat = 90 degrees * vectorLength / R - 90 degrees
		val latitude = South.radians(math.Pi * vector.length / (equatorVectorRadius * 2.0) - (math.Pi / 2.0))
		// Longitude matches the vector direction exactly
		LatLong(latitude, vector.direction)
	}
	
	
	// OTHER    ---------------------
	
	/**
	  * Calculates the travel distance of a latitude shift
	  * @param latitudeShift A shift in latitude coordinates
	  * @return The distance traveled during that coordinate-shift.
	  *         NB: Always positive.
	  */
	// Travel length (VL) = lat / 90 degrees * R
	def latitudeTravelDistance(latitudeShift: NorthSouthRotation): WorldDistance =
		distance(latitudeShift.absolute.quarters * equatorVectorRadius)
	/**
	  * @param southTravelDistance Amount of distance traveled towards the south
	  * @return A latitude shift caused by that travel
	  */
	// Lat = VL * 90 degrees / R
	def travelDistanceToLatitude(southTravelDistance: Double) =
		South.quarters(southTravelDistance / equatorVectorRadius)
	
	/**
	  * Calculates the east west circle radius at a specific latitude level
	  * @param latitude Targeted latitude level
	  * @return The radius of the east-to-west circle at that latitude level, where the circle origin lies
	  *         at the north pole (i.e. 90 degrees north)
	  */
	def radiusAtLatitude(latitude: NorthSouthRotation): WorldDistance = {
		// Latitude travel (0 degrees south) starts from the position of +90 degrees, when observed from the
		// center of the circle (i.e. North 90 degrees)
		latitudeTravelDistance(latitude + South.quarter)
	}
}

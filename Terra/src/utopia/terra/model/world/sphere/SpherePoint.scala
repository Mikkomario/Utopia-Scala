package utopia.terra.model.world.sphere

import utopia.flow.operator.EqualsBy
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.distance.{AerialHaversineDistanceOps, DistanceOps}
import utopia.terra.controller.coordinate.world.SphericalEarth
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.AerialPoint

object SpherePoint
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * Logic for calculating distance between points of this type
	 */
	implicit val distanceOps: DistanceOps[SpherePoint] = AerialHaversineDistanceOps
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param surfacePoint A point on the Earth sphere's surface
	 * @param altitude Altitude above the sea level
	 * @return A point on the spherical Earth system
	 */
	def apply(surfacePoint: SphereSurfacePoint, altitude: Distance): SpherePoint =
		new _SpherePoint(surfacePoint, altitude)
	
	/**
	 * @param latLong A latitude-longitude coordinate
	 * @param altitude Altitude above the sea level
	 * @return A point matching those coordinates
	 */
	def apply(latLong: LatLong, altitude: Distance): SpherePoint = apply(SphereSurfacePoint(latLong), altitude)
	
	/**
	 * @param vector A vector in the spherical Earth system
	 * @return A point matching that vector
	 */
	def apply(vector: Vector3D): SpherePoint = new VectorOverSphere(vector)
	
	
	// NESTED   ----------------------------
	
	class _SpherePoint(surfacePoint: SphereSurfacePoint, override val altitude: Distance) extends SpherePoint
	{
		// ATTRIBUTES   ----------------------
		
		override lazy val vector: Vector3D = SphericalEarth.changeAltitude(surfacePoint.vector, altitude)
		
		
		// IMPLEMENTED  ---------------------
		
		override def latLong: LatLong = surfacePoint.latLong
		override def toSurfacePoint: SphereSurfacePoint = surfacePoint
	}
	
	class VectorOverSphere(override val vector: Vector3D) extends SpherePoint
	{
		// ATTRIBUTES   --------------------
		
		override lazy val latLong: LatLong = SphericalEarth.vectorToLatLong(vector)
		override lazy val altitude: Distance = SphericalEarth.altitudeAt(vector)
		
		
		// IMPLEMENTED  --------------------
		
		override def toSurfacePoint: SphereSurfacePoint = AtSurface
		
		
		// NESTED   ------------------------
		
		object AtSurface extends SphereSurfacePoint
		{
			// ATTRIBUTES   ----------------
			
			override lazy val vector: Vector3D = VectorOverSphere.this.vector.withLength(1.0)
			
			
			// IMPLEMENTED  ----------------
			
			override def latLong: LatLong = VectorOverSphere.this.latLong
			
			override def withAltitude(altitude: Distance): SpherePoint = SpherePoint(this, altitude)
		}
	}
}

/**
 * Represents a specific location in the spherical Earth system.
 * See [[SphericalEarth]] for details about the coordinate system used.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
trait SpherePoint extends AerialPoint[Vector3D, SphereSurfacePoint] with EqualsBy
{
	override protected def equalsProperties: Iterable[Any] = Iterable.single(vector)
}

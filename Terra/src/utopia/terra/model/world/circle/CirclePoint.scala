package utopia.terra.model.world.circle

import utopia.flow.operator.EqualsBy
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.distance.{DistanceOps, VectorDistanceOps}
import utopia.terra.controller.coordinate.world.CircleOfEarth
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.AerialPoint

object CirclePoint
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * Algorithm used for calculating distances between two points of this kind
	 */
	implicit val distanceOps: DistanceOps[CirclePoint] = new VectorDistanceOps[Vector3D](CircleOfEarth.unitDistance)
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param surfacePoint A surface level point
	 * @param altitude Altitude above the sea level
	 * @return A 3D point over the circular plane
	 */
	def apply(surfacePoint: CircleSurfacePoint, altitude: Distance): CirclePoint =
		new _CirclePoint(surfacePoint, altitude)
	
	/**
	 * @param latLong Latitude-longitude coordinate
	 * @param altitude Altitude above the sea level
	 * @return A 3D point matching those coordinates
	 */
	def apply(latLong: LatLong, altitude: Distance): CirclePoint =
		new _CirclePoint(CircleSurfacePoint(latLong), altitude)
	
	/**
	 * @param vector A 3D vector on the Circle of Earth -system
	 * @return A point that matches the specified vector
	 */
	def apply(vector: Vector3D): CirclePoint = new VectorOnCircle(vector)
	
	
	// NESTED   ----------------------------
	
	class _CirclePoint(surfacePoint: CircleSurfacePoint, override val altitude: Distance) extends CirclePoint
	{
		// ATTRIBUTES   --------------------
		
		override lazy val vector: Vector3D =
			Vector3D(surfacePoint.vector.dimensions.withZ(CircleOfEarth.vectorLengthOf(altitude)))
		
		
		// IMPLEMENTED  --------------------
		
		override def latLong: LatLong = surfacePoint.latLong
		override def toSurfacePoint: CircleSurfacePoint = surfacePoint
	}
	
	class VectorOnCircle(override val vector: Vector3D) extends CirclePoint
	{
		// ATTRIBUTES   ---------------------
		
		override lazy val toSurfacePoint: CircleSurfacePoint = CircleSurfacePoint(vector.in2D)
		override lazy val altitude: Distance = CircleOfEarth.vectorLengthToDistance(vector.z)
		
		
		// COMPUTED ------------------------
		
		override def latLong: LatLong = toSurfacePoint.latLong
	}
}

/**
 * Represents a 3-dimensional point on the "Circle of Earth".
 *
 * The (0,0,0) vector lies at the "real" north pole, under the Polaris at sea level.
 * Unit vector length (1.0) represents the distance from the north pole to the equator.
 * The z-axis runs parallel to the Earth's surface, towards the Heaven.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
trait CirclePoint extends AerialPoint[Vector3D, CircleSurfacePoint] with EqualsBy
{
	override protected def equalsProperties: Iterable[Any] = Iterable.single(vector)
}

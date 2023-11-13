package utopia.terra.model.world.circle

import utopia.flow.operator.EqualsBy
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.terra.controller.coordinate.distance.{DistanceOps, VectorDistanceOps}
import utopia.terra.controller.coordinate.world.{CircleOfEarth, LatLongToWorldPoint, VectorToWorldPoint}
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.{AerialPoint, WorldDistance}

object AerialCirclePoint
	extends VectorToWorldPoint[Vector2D, Vector3D, CircleSurfacePoint, AerialCirclePoint]
		with LatLongToWorldPoint[CircleSurfacePoint, AerialCirclePoint]
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * Algorithm used for calculating distances between two points of this kind
	 */
	implicit val distanceOps: DistanceOps[AerialCirclePoint] = new VectorDistanceOps[Vector3D](CircleOfEarth.unitDistance)
	
	
	// IMPLEMENTED  ------------------------
	
	override protected implicit def worldView: CircleOfEarth.type = CircleOfEarth
	
	override def apply(latLong: LatLong): CircleSurfacePoint = CircleSurfacePoint(latLong)
	override def apply(latLong: LatLong, altitude: WorldDistance): AerialCirclePoint =
		new _CirclePoint(CircleSurfacePoint(latLong), altitude)
	
	override def surfaceVector(vector: Vector2D): CircleSurfacePoint = CircleSurfacePoint(vector)
	override def aerialVector(vector: Vector3D): AerialCirclePoint = apply(vector)
	
	
	// OTHER    ----------------------------
	
	/**
	 * @param surfacePoint A surface level point
	 * @param altitude Altitude above the sea level
	 * @return A 3D point over the circular plane
	 */
	def apply(surfacePoint: CircleSurfacePoint, altitude: WorldDistance): AerialCirclePoint =
		new _CirclePoint(surfacePoint, altitude)
	
	/**
	 * @param vector A 3D vector on the Circle of Earth -system
	 * @return A point that matches the specified vector
	 */
	def apply(vector: Vector3D): AerialCirclePoint = new VectorOnCircle(vector)
	
	
	// NESTED   ----------------------------
	
	private class _CirclePoint(surfacePoint: CircleSurfacePoint, override val altitude: WorldDistance)
		extends AerialCirclePoint
	{
		// ATTRIBUTES   --------------------
		
		override lazy val vector: Vector3D = {
			val v2D = surfacePoint.vector
			Vector3D(v2D.x, v2D.y, altitude.vectorLength)
		}
		
		
		// IMPLEMENTED  --------------------
		
		override def latLong: LatLong = surfacePoint.latLong
		override def toSurfacePoint: CircleSurfacePoint = surfacePoint
		
		override def withAltitude(altitude: Distance): AerialCirclePoint = new _CirclePoint(surfacePoint, altitude)
		override def withAltitude(altitude: Double): AerialCirclePoint = new _CirclePoint(surfacePoint, altitude)
	}
	
	class VectorOnCircle(override val vector: Vector3D) extends AerialCirclePoint
	{
		// ATTRIBUTES   ---------------------
		
		override lazy val toSurfacePoint: CircleSurfacePoint = CircleSurfacePoint(vector.in2D)
		override lazy val altitude = CircleOfEarth.distance(vector.z)
		
		
		// COMPUTED ------------------------
		
		override def latLong: LatLong = toSurfacePoint.latLong
		
		
		// IMPLEMENTED  --------------------
		
		override def withAltitude(altitude: Distance): AerialCirclePoint = withAltitude(worldView.vectorLengthOf(altitude))
		override def withAltitude(altitude: Double): AerialCirclePoint = new VectorOnCircle(vector.withZ(altitude))
	}
}

/**
 * Represents a 3-dimensional point on the "Circle of Earth".
 *
 * The (0,0,0) vector lies at the "real" north pole, under the Polaris at sea level.
 * Length of 100 000 represents the distance from the north pole to the equator.
 * The z-axis runs parallel to the Earth's surface, towards the Heaven.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
trait AerialCirclePoint
	extends AerialPoint[Vector3D, AerialCirclePoint, DoubleVector, CircleSurfacePoint, AerialCircleTravel]
		with CirclePointOps[Vector3D, AerialCirclePoint, AerialCirclePoint, AerialCircleTravel] with EqualsBy
{
	override protected def at(latLong: LatLong): AerialCirclePoint = AerialCirclePoint(CircleSurfacePoint(latLong), altitude)
	override protected def at(location: HasDoubleDimensions): AerialCirclePoint = AerialCirclePoint(Vector3D.from(location))
	
	override def to(target: AerialCirclePoint): AerialCircleTravel = AerialCircleTravel(this, target)
	override def -(origin: AerialCirclePoint): AerialCircleTravel = AerialCircleTravel(origin, this)
}

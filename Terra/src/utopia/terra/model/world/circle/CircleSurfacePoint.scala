package utopia.terra.model.world.circle

import utopia.flow.operator.equality.EqualsBy
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.terra.controller.coordinate.distance.{DistanceOps, VectorDistanceOps}
import utopia.terra.controller.coordinate.world.{CircleOfEarth, LatLongToSurfacePoint, VectorToSurfacePoint}
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.SurfacePoint

object CircleSurfacePoint
	extends VectorToSurfacePoint[Vector2D, CircleSurfacePoint] with LatLongToSurfacePoint[CircleSurfacePoint]
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * Algorithm used for calculating distances between points of this kind
	 */
	implicit val distanceOps: DistanceOps[CircleSurfacePoint] =
		new VectorDistanceOps[Vector2D](CircleOfEarth.unitDistance)
	
	/**
	  * The (geometric) north pole location
	  */
	val northPole = apply(Vector2D.zero)
		
	
	// IMPLEMENTED  ---------------------
	
	override def apply(latLong: LatLong): CircleSurfacePoint = new LatLongOnCircle(latLong)
	override def surfaceVector(vector: Vector2D): CircleSurfacePoint = apply(vector)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param vector A vector in the Circle of Earth -system
	 * @return A point representation of that vector
	 */
	def apply(vector: Vector2D): CircleSurfacePoint = new VectorOnCircle(vector)
	
	
	// NESTED   -------------------------
	
	private class LatLongOnCircle(override val latLong: LatLong) extends CircleSurfacePoint
	{
		override lazy val vector: Vector2D = CircleOfEarth.latLongToVector(latLong)
	}
	
	private class VectorOnCircle(override val vector: Vector2D) extends CircleSurfacePoint
	{
		override lazy val latLong: LatLong = CircleOfEarth.vectorToLatLong(vector)
	}
}

/**
 * Represents a point on the "Circle of Earth".
 *
 * The (0,0) vector coordinate lies on the "real" north pole, under the Polaris.
 * Length of 100 000 represents the distance from the north pole to the equator.
 * The X axis runs along the 0 longitude line (Greenwich, England) towards south (right).
 *
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
trait CircleSurfacePoint
	extends SurfacePoint[Vector2D, CircleSurfacePoint, DoubleVector, AerialCirclePoint, CircleSurfaceTravel]
		with CirclePointOps[Vector2D, CircleSurfacePoint, AerialCirclePoint, CircleSurfaceTravel] with EqualsBy
{
	override def at(latLong: LatLong): CircleSurfacePoint = CircleSurfacePoint(latLong)
	override protected def at(location: HasDoubleDimensions): CircleSurfacePoint =
		CircleSurfacePoint(Vector2D.from(location))
	
	override def withAltitude(altitude: Distance): AerialCirclePoint = AerialCirclePoint(this, altitude)
	override def withAltitude(altitude: Double): AerialCirclePoint = AerialCirclePoint(this, altitude)
	
	override def to(target: CircleSurfacePoint): CircleSurfaceTravel = CircleSurfaceTravel(this, target)
	override def -(origin: CircleSurfacePoint): CircleSurfaceTravel = CircleSurfaceTravel(origin, this)
}
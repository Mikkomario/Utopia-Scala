package utopia.terra.model.vector.circle

import utopia.flow.operator.EqualsBy
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.terra.controller.coordinate.{CircleOfEarth, DistanceOps, VectorDistanceOps}
import utopia.terra.model.angular.LatLong
import utopia.terra.model.vector.SurfacePoint

object CircleSurfacePoint
{
	// ATTRIBUTES   ---------------------
	
	/**
	 * Algorithm used for calculating distances between points of this kind
	 */
	implicit val distanceOps: DistanceOps[CircleSurfacePoint] =
		new VectorDistanceOps[Vector2D](CircleOfEarth.unitDistance)
	
	
	// OTHER    -------------------------
	
	/**
	 * @param latLong A latitude-longitude coordinate
	 * @return That point on the Circle of Earth -system
	 */
	def apply(latLong: LatLong): CircleSurfacePoint = new LatLongOnCircle(latLong)
	/**
	 * @param vector A vector in the Circle of Earth -system
	 * @return A point representation of that vector
	 */
	def apply(vector: Vector2D): CircleSurfacePoint = new CircleVector(vector)
	
	
	// NESTED   -------------------------
	
	private class LatLongOnCircle(override val latLong: LatLong) extends CircleSurfacePoint
	{
		override lazy val vector: Vector2D = CircleOfEarth.latLongToVector(latLong)
	}
	
	private class CircleVector(override val vector: Vector2D) extends CircleSurfacePoint
	{
		override lazy val latLong: LatLong = CircleOfEarth.vectorToLatLong(vector)
	}
}

/**
 * Represents a point on the "Circle of Earth".
 *
 * The (0,0) vector coordinate lies on the "real" north pole, under the Polaris.
 * A unit length (1.0) represents the distance from the north pole to the equator.
 * The X axis runs along the 0 longitude line (Greenwich, England) towards south (right).
 *
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
trait CircleSurfacePoint
	extends SurfacePoint[Vector2D, CirclePoint] with EqualsBy
{
	override protected def equalsProperties: Iterable[Any] = Iterable.single(vector)
	
	override def withAltitude(altitude: Distance): CirclePoint = CirclePoint(this, altitude)
}
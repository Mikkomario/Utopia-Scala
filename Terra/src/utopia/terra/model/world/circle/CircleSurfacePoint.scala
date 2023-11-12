package utopia.terra.model.world.circle

import utopia.flow.operator.EqualsBy
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.terra.controller.coordinate.distance.{DistanceOps, VectorDistanceOps}
import utopia.terra.controller.coordinate.world.{CircleOfEarth, LatLongToSurfacePoint, VectorToSurfacePoint}
import utopia.terra.model.CompassTravel
import utopia.terra.model.angular.LatLong
import utopia.terra.model.enumeration.CompassDirection.{EastWest, NorthSouth, West}
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
 * Length of 100 000 represents the distance from the north pole to the equator.
 * The X axis runs along the 0 longitude line (Greenwich, England) towards south (right).
 *
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
trait CircleSurfacePoint
	extends SurfacePoint[Vector2D, CirclePoint] with CirclePointOps[Vector2D, CircleSurfacePoint] with EqualsBy
{
	override def withAltitude(altitude: Distance): CirclePoint = CirclePoint(this, altitude)
	override protected def at(latLong: LatLong): CircleSurfacePoint = CircleSurfacePoint(latLong)
	override protected def at(location: HasDoubleDimensions): CircleSurfacePoint =
		CircleSurfacePoint(Vector2D.from(location))
	
	override def +(travel: CompassTravel): CircleSurfacePoint = {
		val travelVectorLength = worldView.vectorLengthOf(travel.distance)
		travel.compassAxis match {
			// Case: North-South travel => Linear travel path towards or away from the center
			case NorthSouth => CircleSurfacePoint(vector + vector.withLength(travelVectorLength))
			// Case: East-West travel => Arcing travel staying at the same latitude line
			case EastWest => this + West.forArcLength(travelVectorLength, vector.length)
		}
	}
}
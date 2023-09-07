package utopia.terra.model.world.sphere

import utopia.flow.operator.EqualsBy
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.terra.controller.coordinate.distance.{DistanceOps, SurfaceHaversineDistanceOps}
import utopia.terra.controller.coordinate.world.{LatLongToSurfacePoint, SphericalEarth}
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.SurfacePoint

object SphereSurfacePoint
	extends LatLongToSurfacePoint[SphereSurfacePoint]
{
	// ATTRIBUTES   ------------------------
	
	/**
	 * Algorithm for calculating distances between sphere surface points.
	 * Assumes travelling to occur on the mean sea level.
	 */
	implicit val distanceOps: DistanceOps[SphereSurfacePoint] = SurfaceHaversineDistanceOps.atMeanSeaLevel
	
	
	// IMPLEMENTED  ------------------------
	
	/**
	 * @param latLong A latitude-longitude coordinate
	 * @return a point on the spherical Earth's surface (i.e. at the sea level) that matches those coordinates
	 */
	override def apply(latLong: LatLong): SphereSurfacePoint = new LatLongOnSphere(latLong)
	
	
	// NESTED   ----------------------------
	
	class LatLongOnSphere(override val latLong: LatLong) extends SphereSurfacePoint
	{
		override lazy val vector: Vector3D = SphericalEarth.latLongToVector(latLong)
		
		override def withAltitude(altitude: Distance): SpherePoint = SpherePoint(this, altitude)
	}
}

/**
 * Represents a point on the Globe's (i.e. spherical Earth's) surface.
 * Does not take into account any potential oblation or flattening. Assumes a fully spherical Earth.
 *
 * In the vector form, (0,0,0) lies at the center of the Earth sphere.
 * The Z-vector pierces the sphere through the south and the north poles (where north is positive and south is negative).
 * The X-Y plane (Z=0) covers the whole equator.
 * X-axis intersects with the equator (on the positive side) at 0 degree longitude coordinates.
 * Positive longitude moves from east to the west.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
trait SphereSurfacePoint
	extends SurfacePoint[Vector3D, SpherePoint]
		with SpherePointOps[SurfacePoint[HasDoubleDimensions, _], SphereSurfacePoint] with EqualsBy
{
	override protected def equalsProperties: Iterable[Any] = Iterable.single(vector)
	
	override def arcingDistanceFrom(other: SurfacePoint[HasDoubleDimensions, _]): Distance =
		SurfaceHaversineDistanceOps.atMeanSeaLevel.distanceBetween(other, this)
	
	override protected def at(latLong: LatLong): SphereSurfacePoint = SphereSurfacePoint(latLong)
}

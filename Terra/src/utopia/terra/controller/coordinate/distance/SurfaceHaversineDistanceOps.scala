package utopia.terra.controller.coordinate.distance

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.measurement.Distance
import utopia.terra.controller.coordinate.GlobeMath
import utopia.terra.model.world.SurfacePoint

object SurfaceHaversineDistanceOps
{
	/**
	 * @return An algorithm for calculating distances, assuming a perfectly spherical Earth..
	 *         Assumes that travelling occurs at the mean sea level
	 */
	def atMeanSeaLevel = new SurfaceHaversineDistanceOps(GlobeMath.meanRadius)
	
	/**
	 * @param altitude Travel altitude above the mean sea level
	 * @return An algorithm for calculating distances, assuming a perfectly spherical Earth.
	 */
	def atAltitude(altitude: Distance) = new SurfaceHaversineDistanceOps(GlobeMath.meanRadius + altitude)
}

/**
 * Logic for calculating the distance between two points.
 * Assumes a perfectly spherical Earth and arcing travel paths.
 * Assumes that the travelling occurs on a static radius.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
class SurfaceHaversineDistanceOps(radius: Distance) extends DistanceOps[SurfacePoint[Any, Any]]
{
	// ATTRIBUTES   ------------------------
	
	private val haversine = new HaversineDistanceOps(radius)
	
	
	// IMPLEMENTED  ------------------------
	
	override def distanceBetween(points: Pair[SurfacePoint[Any, Any]]): Distance =
		haversine.distanceBetween(points.map { _.latLong })
}

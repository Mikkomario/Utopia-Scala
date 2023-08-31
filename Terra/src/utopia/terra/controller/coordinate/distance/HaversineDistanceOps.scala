package utopia.terra.controller.coordinate.distance

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.measurement.Distance
import utopia.terra.model.angular.LatLong

/**
 * Used for calculating arc length distance between latitude-longitude coordinates.
 * Assumes a perfectly round spherical Earth and arcing trajectory.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @param radius Assumed radius from the center of the Earth sphere to the traversal arc height
 */
class HaversineDistanceOps(radius: Distance) extends DistanceOps[LatLong]
{
	override def distanceBetween(points: Pair[LatLong]): Distance = {
		import math._
		
		val latitudes = points.map { _.latitude.radians }
		val longitudes = points.map { _.longitude.radians }
		
		val deltaLat = latitudes.merge { _ - _ }
		def deltaLon = longitudes.merge { _ - _ }
		
		val k = pow(sin(deltaLat/2), 2) + latitudes.map(cos).merge { _ * _ } * pow(sin(deltaLon/2), 2)
		val c = 2 * atan2(sqrt(k), sqrt(1-k))
		
		radius * c
	}
}

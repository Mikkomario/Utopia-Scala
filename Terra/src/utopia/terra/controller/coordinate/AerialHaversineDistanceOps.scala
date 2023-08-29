package utopia.terra.controller.coordinate

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.measurement.Distance
import utopia.paradigm.measurement.DistanceUnit.Meter
import utopia.terra.model.vector.AerialPoint

/**
 * Used for calculating travel distances between two locations which include altitude information.
 * Assumes a perfectly spherical Earth.
 * The distance calculation is not exact when altitudes vary.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
object AerialHaversineDistanceOps extends DistanceOps[AerialPoint[Any, Any]]
{
	override def distanceBetween(points: Pair[AerialPoint[Any, Any]]): Distance = {
		// Uses a mean radius, giving a greater emphasis on the higher radius value
		// (2a + b) / 3, where a is the higher radius and b is the lower radius
		val meanRadius = (points.first.altitude * 2 + points.second.altitude) / 3.0
		
		// Calculates the haversine (arc) distance between the two points at the mean radius
		val arcLength = new HaversineDistanceOps(meanRadius).distanceBetween(points.map { _.latLong })
		
		// Adds slight accounting for the vertical travel, also
		// Uses the Pythagorean theorem
		val verticalDistance = points.map { _.altitude }.merge { _ - _ }
		
		if (verticalDistance == Distance.zero)
			arcLength
		else if (arcLength == Distance.zero)
			verticalDistance
		else {
			import math._
			Distance(sqrt(pow(arcLength.toM, 2) + pow(verticalDistance.toM, 2)), Meter)
		}
	}
}

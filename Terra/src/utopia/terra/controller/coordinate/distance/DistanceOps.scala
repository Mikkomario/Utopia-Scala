package utopia.terra.controller.coordinate.distance

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.measurement.Distance

/**
 * Common trait for distance-calculation implementations
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 */
trait DistanceOps[-P]
{
	// ABSTRACT ---------------------
	
	/**
	 * @param points the two points between which distance is calculated
	 * @return The distance between these two points
	 */
	def distanceBetween(points: Pair[P]): Distance
	
	
	// OTHER    ---------------------
	
	/**
	 * @param a Point A
	 * @param b Point B
	 * @return The distance between these two points
	 */
	def distanceBetween(a: P, b: P): Distance = distanceBetween(Pair(a, b))
}

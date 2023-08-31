package utopia.terra.controller.coordinate.distance

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.{Combinable, HasLength, Reversible}
import utopia.paradigm.measurement.Distance

/**
 * Distance-calculation algorithm for calculating linear distance between two (vector) points
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @param unitDistance Distance of length 1.0 in vector space.
 */
class LinearDistanceOps[V <: HasLength with Reversible[V] with Combinable[V, V]](unitDistance: Distance)
	extends DistanceOps[V]
{
	override def distanceBetween(points: Pair[V]): Distance = _distanceBetween(points.first, points.second)
	override def distanceBetween(a: V, b: V): Distance = _distanceBetween(a, b)
	
	private def _distanceBetween(a: V, b: V) = unitDistance * (b - a).length
}

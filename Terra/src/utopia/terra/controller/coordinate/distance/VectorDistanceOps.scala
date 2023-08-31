package utopia.terra.controller.coordinate.distance

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.{Combinable, HasLength, Reversible}
import utopia.paradigm.measurement.Distance
import utopia.terra.model.world.WorldPoint

/**
 * Distance-calculation algorithm for calculating linear distance between two points using their vector forms.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @param unitDistance Distance of length 1.0 in vector space.
 * @tparam V Type of vector forms used
 */
class VectorDistanceOps[V <: HasLength with Reversible[V] with Combinable[V, V]](unitDistance: Distance)
	extends DistanceOps[WorldPoint[V]]
{
	// ATTRIBUTES   -----------------------
	
	private val linear = new LinearDistanceOps[V](unitDistance)
	
	
	// IMPLEMENTED  -----------------------
	
	override def distanceBetween(points: Pair[WorldPoint[V]]): Distance = linear.distanceBetween(points.map { _.vector })
}

package utopia.terra.controller.coordinate.distance

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.combine.Combinable
import utopia.flow.operator.{HasLength, Reversible}
import utopia.paradigm.measurement.Distance
import utopia.terra.controller.coordinate.world.VectorDistanceConversion
import utopia.terra.model.world.WorldPoint


object VectorDistanceOps
{
	/**
	  * Creates a new vector distance ops -instance using implicitly available scaling information
	  * @param vw Implicit world view to assume
	  * @tparam V Type of vectors compared
	  * @return A new instance for calculating vector distances
	  */
	def implicitly[V <: HasLength with Reversible[V] with Combinable[V, V]](implicit vw: VectorDistanceConversion) =
		new VectorDistanceOps[V](vw.unitDistance)
}

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

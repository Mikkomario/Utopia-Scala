package utopia.terra.model.world

import utopia.flow.operator.Reversible

/**
 * Common trait for coordinate modes that represent a singular point on the Earth.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @tparam V Type of the vector representation of this point
 * @tparam P Type of this point
  * @tparam VI Highest supported comparable vector representation
  * @tparam Aerial Type of the aerial (i.e. 3D) representation of this point
  * @tparam T Type of travel paths created between these points
 */
trait SurfacePoint[+V, P, VI <: Reversible[VI], +Aerial, +T] extends WorldPointOps[V, P, VI, Aerial, T]
{
	override def altitude: WorldDistance = WorldDistance.zero
}

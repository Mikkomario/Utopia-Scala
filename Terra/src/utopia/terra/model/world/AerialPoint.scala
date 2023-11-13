package utopia.terra.model.world

import utopia.flow.operator.Reversible

/**
 * Common trait for points that represent some 3-dimensional point relative to some point on the Earth.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @tparam V Type of the vector representation of this point
 * @tparam P Type of this point
  * @tparam VI Highest comparable vector type
  * @tparam Surface Type of the surface (i.e. 2D) representation of this point
  * @tparam T      Type of travel paths created between these points
 */
trait AerialPoint[+V, P, VI <: Reversible[VI], +Surface, +T] extends WorldPointOps[V, P, VI, P, T]
{
	// ABSTRACT ----------------------
	
	/**
	 * @return A surface-level point that matches this aerial point (without altitude information)
	 */
	def toSurfacePoint: Surface
}

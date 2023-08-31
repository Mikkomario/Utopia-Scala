package utopia.terra.model.world

import utopia.paradigm.measurement.Distance

/**
 * Common trait for points that represent some 3-dimensional point relative to some point on the Earth.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @tparam V Type of the vector representation of this point
 * @tparam Surface Type of the surface (i.e. 2D) representation of this point
 */
trait AerialPoint[+V, +Surface] extends WorldPoint[V]
{
	// ABSTRACT ----------------------
	
	/**
	 * @return The altitude of this point relative to the sea level
	 */
	def altitude: Distance
	
	/**
	 * @return A surface-level point that matches this aerial point (without altitude information)
	 */
	def toSurfacePoint: Surface
}

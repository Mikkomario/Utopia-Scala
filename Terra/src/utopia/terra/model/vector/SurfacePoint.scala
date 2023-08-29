package utopia.terra.model.vector

import utopia.paradigm.measurement.Distance

/**
 * Common trait for coordinate modes that represent a singular point on the Earth.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @tparam V Type of the vector representation of this point
 * @tparam Aerial Type of the aerial (i.e. 3D) representation of this point
 */
trait SurfacePoint[+V, +Aerial] extends WorldPoint[V]
{
	// ABSTRACT ------------------------
	
	/**
	 * @param altitude Altitude given to this surface point
	 * @return An aerial point above or below this surface point
	 */
	def withAltitude(altitude: Distance): Aerial
}

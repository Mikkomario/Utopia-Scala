package utopia.terra.controller.coordinate.world

import utopia.paradigm.measurement.Distance
import utopia.terra.model.world.WorldDistance

/**
 * Common trait for different world representation models used for coordinate transformations
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @tparam V2D Type of the vector representation of surface-level points
 * @tparam V3D Type of the vector representation of aerial points
 * @tparam Surface Surface level (2D) point type
 * @tparam Aerial Aerial (3D) point type
 */
trait WorldView[V2D, -V3D, +Surface, +Aerial]
	extends WorldPointFactory[V2D, V3D, Surface, Aerial]
		with VectorDistanceConversion with LatLongFromVectorFactory[V2D]
		with VectorFromLatLongFactory[V2D]
{
	/**
	  * @param distance A distance travelled
	  * @return Copy of that distance as a "world distance"
	  */
	def distance(distance: Distance): WorldDistance = WorldDistance(distance)(this)
	/**
	  * @param vectorDistance A distance travelled (in vector space)
	  * @return Copy of that distance as a "world distance"
	  */
	def distance(vectorDistance: Double): WorldDistance = WorldDistance.vector(vectorDistance)(this)
}

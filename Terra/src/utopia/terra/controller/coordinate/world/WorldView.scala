package utopia.terra.controller.coordinate.world

import utopia.paradigm.measurement.Distance
import utopia.terra.model.angular.LatLong

/**
 * Common trait for different world representation models used for coordinate transformations
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @tparam V2D Type of the vector representation of surface-level points
 * @tparam V3D Type of the vector representation of aerial points
 * @tparam Surface Surface level (2D) point type
 * @tparam Aerial Aerial (3D) point type
 */
trait WorldView[-V2D, -V3D, +Surface, +Aerial]
{
	/**
	 * @param latLong A latitude-longitude coordinate
	 * @return A point on the Earth's surface that matches that coordinate
	 */
	def apply(latLong: LatLong): Surface
	/**
	 * @param latLong A latitude-longitude coordinate
	 * @param altitude Altitude above the sea level
	 * @return A point within the world that matches this location
	 */
	def apply(latLong: LatLong, altitude: Distance): Aerial
	
	/**
	 * @param vector A vector in this world view system
	 * @return A surface point that matches that vector
	 */
	def surfaceVector(vector: V2D): Surface
	/**
	 * @param vector A vector in this world view system
	 * @return A 3D point that matches that vector
	 */
	def aerialVector(vector: V3D): Aerial
}

package utopia.terra.model.world

import utopia.terra.model.angular.LatLong

/**
 * Common trait for points that represent a specific 2D or 3D location on an Earth system of some kind.
 * @author Mikko Hilpinen
 * @since 29.8.2023, v1.0
 * @tparam V Vector representation of this point
 */
trait WorldPoint[+V]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return A vector representation of this point
	 */
	def vector: V
	/**
	 * @return The latitude-longitude coordinates of this point
	 */
	def latLong: LatLong
}

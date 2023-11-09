package utopia.terra.controller.coordinate.world

import utopia.terra.model.angular.LatLong

/**
  * Common trait for classes that convert vectors to latitude-longitude values
  * @author Mikko Hilpinen
  * @since 8.11.2023, v1.1
  * @tparam V Type of vectors that may be converted to latitude-longitude pairs
  */
trait LatLongFromVectorFactory[-V]
{
	/**
	  * @param vector A vector (world-view -specific)
	  * @return A latitude-longitude coordinate that matches that vector
	  */
	def vectorToLatLong(vector: V): LatLong
}

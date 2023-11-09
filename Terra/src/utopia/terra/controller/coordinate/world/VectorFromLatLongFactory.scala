package utopia.terra.controller.coordinate.world

import utopia.terra.model.angular.LatLong

/**
  * Common trait for classes that convert from latitude-longitude coordinates into
  * vectors.
  * @author Mikko Hilpinen
  * @since 8.11.2023, v1.1
  */
trait VectorFromLatLongFactory[+V]
{
	/**
	  * @param latLong A latitude-longitude coordinate
	  * @return A vector representation of that coordinate
	  */
	def latLongToVector(latLong: LatLong): V
}

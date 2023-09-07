package utopia.terra.controller.coordinate.world

import utopia.terra.model.angular.LatLong

/**
  * Common trait for factory classes that accept latitude-longitude coordinates and return matching world coordinates
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  * @tparam S Type of Earth's surface points produced by this factory
  */
trait LatLongToSurfacePoint[+S]
{
	/**
	  * @param latLong A latitude-longitude coordinate
	  * @return A point on the Earth's surface that matches that coordinate
	  */
	def apply(latLong: LatLong): S
}

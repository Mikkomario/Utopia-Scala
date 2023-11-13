package utopia.terra.controller.coordinate.world

import utopia.paradigm.measurement.Distance
import utopia.terra.model.angular.LatLong
import utopia.terra.model.world.WorldDistance

/**
  * Common trait for factories used for converting latitude-longitude coordinates plus altitude information
  * into aerial world coordinates
  * @author Mikko Hilpinen
  * @since 12.11.2023, v1.1
  * @tparam A Type of aerial world points produced by this factory
  */
trait LatLongToAerialPoint[+A]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The world view assumed by this interface
	  */
	protected implicit def worldView: VectorDistanceConversion
	
	/**
	  * @param latLong  A latitude-longitude coordinate
	  * @param altitude Altitude above the sea level
	  * @return A point within the world that matches this location
	  */
	def apply(latLong: LatLong, altitude: WorldDistance): A
	/**
	  * @param latLong  A latitude-longitude coordinate
	  * @param vectorAltitude Altitude above the sea level (in vector coordinates)
	  * @return A point within the world that matches this location
	  */
	def apply(latLong: LatLong, vectorAltitude: Double): A = apply(latLong, WorldDistance.vector(vectorAltitude))
	/**
	  * @param latLong  A latitude-longitude coordinate
	  * @param altitude Altitude above the sea level
	  * @return A point within the world that matches this location
	  */
	def apply(latLong: LatLong, altitude: Distance): A  = apply(latLong, WorldDistance(altitude))
}

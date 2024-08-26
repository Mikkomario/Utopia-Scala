package utopia.terra.controller.coordinate.world

import utopia.paradigm.measurement.Distance
import utopia.terra.model.world.WorldDistance

/**
  * A system used for converting between vector lengths and real world distances
  * @author Mikko Hilpinen
  * @since 6.9.2023, v1.0
  */
trait VectorDistanceConversion
{
	// ABSTRACT ----------------------------
	
	/**
	  * Distance that matches vector length of 1.0
	  */
	def unitDistance: Distance
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param distance A distance on Earth
	  * @return A vector length matching that distance in this world view
	  */
	def vectorLengthOf(distance: Distance) = distance / unitDistance
	/**
	  * @param vectorLength A vector system length value
	  * @return A real life distance matching that value in this world view
	  */
	def distanceOf(vectorLength: Double) = unitDistance * vectorLength
	
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

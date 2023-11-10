package utopia.terra.model.world

import utopia.flow.operator.{Combinable, Reversible}
import utopia.paradigm.measurement.Distance
import utopia.terra.model.CompassTravel
import utopia.terra.model.angular.{CompassRotation, LatLongRotation}

/**
 * Common trait for world points that specify mathematical and distance functions
 * @author Mikko Hilpinen
 * @since 7.9.2023, v1.0
 * @tparam V Vector representation of this point
  * @tparam WP Type of comparable (world) points
  * @tparam VI Comparable vector type (generic)
  * @tparam Repr Implementing type
 */
trait WorldPointOps[+V, -WP, VI <: Reversible[VI], +Repr]
	extends WorldPoint[V] with Combinable[LatLongRotation, Repr]
{
	// ABSTRACT ------------------------
	
	/**
	  * @param other Another point
	  * @return Linear distance between these two points
	  */
	def linearDistanceFrom(other: WP): Distance
	/**
	  * @param other Another point
	  * @return Distance between these two points when using arcing travel, if applicable.
	  */
	def arcingDistanceFrom(other: WP): Distance
	
	/**
	  * Applies the specified travel to this location.
	  * Whether the travel is considered linear or arcing depends on this context.
	  * @param travel Amount of travel to apply
	  * @return Point at the end of that travel.
	  */
	def +(travel: CompassTravel): Repr
	/**
	  * @param rotation Amount of rotational travel to apply
	  * @return A point at the end of that rotational travel
	  */
	def +(rotation: CompassRotation): Repr
	/**
	  * @param vectorTravel Travel to apply, in vector form
	  * @return A point at the end of the specified travel (from this point)
	  */
	def +(vectorTravel: VI): Repr
	
	
	// OTHER    -----------------------
	
	/**
	  * Applies the specified travel to this location.
	  * Whether the travel is considered linear or arcing depends on this context.
	  * @param travel Amount of travel to apply (in reverse)
	  * @return Point at the end of that reversed travel
	  */
	def -(travel: CompassTravel) = this + (-travel)
	/**
	  * @param rotation Amount of rotational travel to apply (in reverse)
	  * @return A point at the end of that reversed rotational travel
	  */
	def -(rotation: CompassRotation) = this + (-rotation)
	/**
	  * @param vectorTravel Travel to apply in reverse
	  * @return A point at the end of the specified reversed travel
	  */
	def -(vectorTravel: VI) = this + (-vectorTravel)
}

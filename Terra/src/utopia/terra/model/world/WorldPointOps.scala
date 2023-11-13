package utopia.terra.model.world

import utopia.flow.operator.Reversible
import utopia.flow.operator.combine.Combinable
import utopia.paradigm.measurement.Distance
import utopia.terra.controller.coordinate.world.VectorDistanceConversion
import utopia.terra.model.CompassTravel
import utopia.terra.model.angular.{CompassRotation, LatLong, LatLongRotation}

/**
 * Common trait for world points that specify mathematical and distance functions
 * @author Mikko Hilpinen
 * @since 7.9.2023, v1.0
 * @tparam V Vector representation of this point
  * @tparam P Thea actual type of implementing points. Also used in path creation.
  * @tparam VI Comparable vector type (generic)
  * @tparam Aerial Type of this point when altitude is non-zero
  * @tparam T Type of travel paths formed between these points
 */
trait WorldPointOps[+V, P, VI <: Reversible[VI], +Aerial, +T]
	extends WorldPoint[V] with Combinable[LatLongRotation, P]
{
	// ABSTRACT ------------------------
	
	/**
	  * @return World view assumed by these world points
	  */
	protected implicit def worldView: VectorDistanceConversion
	
	/**
	  * @return The altitude component of this location
	  */
	def altitude: WorldDistance
	
	/**
	  * @param target Travel target location
	  * @return Travel from this location to 'target'
	  */
	def to(target: P): T
	/**
	  * @param origin Travel start location
	  * @return Travel from 'origin' to this location
	  */
	def -(origin: P): T
	
	/**
	  * @param surfaceCoordinates New coordinates to assign to this point
	  * @return Copy of this point at the specified location. Altitude is preserved.
	  */
	def at(surfaceCoordinates: LatLong): P
	/**
	  * @param altitude New altitude to assign
	  * @return Copy of this point at the specified altitude
	  */
	def withAltitude(altitude: Distance): Aerial
	/**
	  * @param altitude New altitude to assign (in vector coordinates)
	  * @return Copy of this point at the specified altitude
	  */
	def withAltitude(altitude: Double): Aerial
	
	/**
	  * Applies the specified travel to this location.
	  * Whether the travel is considered linear or arcing depends on this context.
	  * @param travel Amount of travel to apply
	  * @return Point at the end of that travel.
	  */
	def +(travel: CompassTravel): P
	/**
	  * @param vectorTravel Travel to apply, in vector form
	  * @return A point at the end of the specified travel (from this point)
	  */
	def +(vectorTravel: VI): P
	
	
	// OTHER    -----------------------
	
	/**
	  * @param altitudeGain Amount of altitude gained
	  * @return Copy of this point after the specified altitude gain
	  */
	def soarBy(altitudeGain: Distance) = withAltitude(altitude.distance + altitudeGain)
	/**
	  * @param altitudeGain Amount of altitude gained (in vector coordinates)
	  * @return Copy of this point after the specified altitude gain
	  */
	def soarBy(altitudeGain: Double) = withAltitude(altitude.vectorLength + altitudeGain)
	/**
	  * @param altitudeLoss Amount of altitude lost
	  * @return Copy of this point after the specified loss of altitude
	  */
	def descendBy(altitudeLoss: Distance) = withAltitude(altitude.distance - altitudeLoss)
	/**
	  * @param altitudeLoss Amount of altitude lost (in vector coordinates)
	  * @return Copy of this point after the specified loss of altitude
	  */
	def descendBy(altitudeLoss: Double) = withAltitude(altitude.vectorLength - altitudeLoss)
	
	/**
	  * @param rotation Amount of rotational travel to apply
	  * @return A point at the end of that rotational travel
	  */
	def +(rotation: CompassRotation): P = at(latLong + rotation)
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

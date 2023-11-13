package utopia.terra.model.world.circle

import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.terra.controller.coordinate.world.CircleOfEarth
import utopia.terra.model.CompassTravel
import utopia.terra.model.angular.EastWestRotation
import utopia.terra.model.enumeration.CompassDirection.{EastWest, NorthSouth}
import utopia.terra.model.world.DoubleWorldPointOps

/**
  * Common trait for Circle of Earth -world points that specify mathematical equations
  * @author Mikko Hilpinen
  * @since 7.9.2023, v1.0
  * @tparam V Type of vectors used by these points
  * @tparam P Type of this point
  * @tparam Aerial Type of aerial copies of this point
  * @tparam T Type of travel paths between these points
  */
trait CirclePointOps[V <: DoubleVectorLike[V], P, +Aerial, +T] extends DoubleWorldPointOps[V, P, Aerial, T]
{
	// IMPLICIT --------------------------
	
	override protected implicit def worldView: CircleOfEarth.type = CircleOfEarth
	
	
	// IMPLEMENTED  ----------------------
	
	override def +(travel: CompassTravel): P = travel.compassAxis match {
		// Case: North or south travel => Moves towards or away from the center point
		case NorthSouth =>
			val latitudeShift = worldView.travelDistanceToLatitude(travel.distance.vectorLength)
			at(latLong + latitudeShift)
		// Case: East or west travel => Moves on the east-to-west circle
		case EastWest =>
			val longitudeShift = EastWestRotation
				.forArcLength(travel.distance.vectorLength, worldView.radiusAtLatitude(latitude).vectorLength)
			at(latLong + longitudeShift)
	}
}

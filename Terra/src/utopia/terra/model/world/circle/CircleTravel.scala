package utopia.terra.model.world.circle

import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorLike}
import utopia.terra.controller.coordinate.world.CircleOfEarth
import utopia.terra.model.world.{Travel, WorldDistance}

/**
  * Common trait for classes that represent travel on the
  * Circle of Earth -model
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  * @tparam P Type of world points used in this travel
  * @tparam V Type of vector representations used in the world points
  */
trait CircleTravel[P <: CirclePointOps[V, P, _, _], V <: DoubleVectorLike[V] with DoubleVector]
	extends Travel[Double, P, V, DoubleVector]
{
	// IMPLEMENTED  ---------------------
	
	override implicit protected def worldView: CircleOfEarth.type = CircleOfEarth
	
	override def arcingDistance: WorldDistance = {
		// Divides the travel into latitude and longitude components
		// Applies arcing travel to the longitude component and
		// linear travel to the latitude component
		val ll = rotation
		// 90 degrees of travel = R
		// => Travel length (VL) = lat / 90 degrees * R
		val latitudeTravel = CircleOfEarth.latitudeTravelDistance(ll.northSouth)
		val averageLatitudeRadius = CircleOfEarth.radiusAtLatitude(middlePoint.latitude)
		val longitudeVectorDistance = ll.eastWest.absolute.arcLengthOver(averageLatitudeRadius.vectorLength)
		// The two components are then combined together using the Pythagorean theorem
		// C^2 = A^2 + B^2
		// => Travel = sqrt( latTravel^2 + lonTravel^2 )
		math.sqrt(math.pow(latitudeTravel.vectorLength, 2) + math.pow(longitudeVectorDistance, 2))
	}
	
	override def apply(progress: Double): P = linearProgress(progress)
}

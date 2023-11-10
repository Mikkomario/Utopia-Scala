package utopia.terra.model.world.circle

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorLike}
import utopia.terra.controller.coordinate.world.CircleOfEarth
import utopia.terra.model.angular.LatLongRotation
import utopia.terra.model.world.{Travel, WorldDistance, WorldPoint, WorldPointOps}

/**
  * Common trait for classes that represent travel on the
  * Circle of Earth -model
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  */
trait CircleTravel[+P <: WorldPointOps[V, _, DoubleVector, P], V <: DoubleVectorLike[V] with DoubleVector]
	extends Travel[Double, P, DoubleVector, V]
{
	override def arcingDistance: WorldDistance = {
		// Divides the travel into latitude and longitude components
		// Applies arcing travel to the longitude component and
		// linear travel to the latitude component
		val ll = rotation
		// 90 degrees of travel = R
		// => Vl = lat / 90 degrees * R
		val latitudeVectorDistance = CircleOfEarth.equatorVectorRadius *
			ll.northSouth.absoluteRadians * 2.0 / math.Pi
		val averageLatitudeVectorDistance = middlePoint.latitude
		val longitudeVectorDistance = ll.eastWest.absoluteArcLengthOver(latitudeVectorDistance)
		???
	}
}

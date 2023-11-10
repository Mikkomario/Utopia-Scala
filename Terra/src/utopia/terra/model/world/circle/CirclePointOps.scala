package utopia.terra.model.world.circle

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorLike}
import utopia.terra.controller.coordinate.world.CircleOfEarth
import utopia.terra.model.angular.{CompassRotation, LatLong, LatLongRotation}
import utopia.terra.model.world.{WorldPoint, WorldPointOps}
/**
  * Common trait for Circle of Earth -world points that specify mathematical equations
  * @author Mikko Hilpinen
  * @since 7.9.2023, v1.0
  */
trait CirclePointOps[V <: DoubleVectorLike[V] with DoubleVector, +Repr]
	extends WorldPointOps[V, WorldPoint[HasDoubleDimensions], DoubleVector, Repr]
{
	// ABSTRACT --------------------------
	
	/**
	  * @param latLong New latitude-longitude coordinates
	  * @return Copy of this point over those coordinates
	  */
	protected def at(latLong: LatLong): Repr
	
	
	// IMPLICIT --------------------------
	
	/**
	  * @return Implied world view in this context
	  */
	protected implicit def worldView: CircleOfEarth.type = CircleOfEarth
	
	
	// IMPLEMENTED  ----------------------
	
	override def toString = s"$latLong / $vector"
	
	override def linearDistanceFrom(other: WorldPoint[HasDoubleDimensions]): Distance =
		worldView.distanceOf((vector - other.vector).length)
	override def arcingDistanceFrom(other: WorldPoint[HasDoubleDimensions]): Distance = linearDistanceFrom(other)
	
	override def +(rotation: CompassRotation): Repr = at(latLong + rotation)
	override def +(other: LatLongRotation): Repr = at(latLong + other)
}

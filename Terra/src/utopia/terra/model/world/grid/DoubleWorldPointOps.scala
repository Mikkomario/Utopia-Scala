package utopia.terra.model.world.grid

import utopia.flow.operator.EqualsBy
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorLike}
import utopia.terra.controller.coordinate.world.VectorDistanceConversion
import utopia.terra.model.angular.{CompassRotation, LatLong, LatLongRotation}
import utopia.terra.model.world.{WorldPoint, WorldPointOps}

/**
  * Common trait for world point classes that provide operative functions and use double number vectors
  * @author Mikko Hilpinen
  * @since 12.11.2023, v1.1
  * @tparam V Type of vectors used by this world point type
  * @tparam WP Highest type of comparable world points
  * @tparam Repr Implementing type
  */
trait DoubleWorldPointOps[+V <: DoubleVectorLike[V], -WP <: WorldPoint[HasDoubleDimensions], +Repr]
	extends WorldPointOps[V, WP, DoubleVector, Repr] with EqualsBy
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The world view assumed by this point class
	  */
	protected def worldView: VectorDistanceConversion
	
	/**
	  * @param location The new location to assign.
	  *                 Based on the existing vector form of this point, should contain the same number of
	  *                 dimensions and likely be of the same type.
	  * @return Copy of this point at the specified location
	  */
	protected def at(location: HasDoubleDimensions): Repr
	/**
	  * Assigns a new surface location to this point. Preserves altitude, if applicable.
	  * @param latLong The new surface location to assign
	  * @return Copy of this point at the specified surface location
	  */
	protected def at(latLong: LatLong): Repr
	
	
	// IMPLEMENTED  ------------------
	
	override protected def equalsProperties: Iterable[Any] = Vector(vector, worldView)
	
	override def linearDistanceFrom(other: WP): Distance = worldView.distanceOf((vector - other.vector).length)
	
	override def +(rotation: CompassRotation): Repr = at(latLong + rotation)
	override def +(other: LatLongRotation): Repr = at(latLong + other)
	override def +(vectorTravel: DoubleVector): Repr = at(vector + vectorTravel)
}

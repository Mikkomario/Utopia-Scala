package utopia.terra.model.world.grid

import utopia.flow.operator.EqualsBy
import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorLike}
import utopia.terra.controller.coordinate.world.GridArea
import utopia.terra.model.CompassTravel
import utopia.terra.model.angular.{CompassRotation, LatLong, LatLongRotation}
import utopia.terra.model.world.{WorldPoint, WorldPointOps}

/**
  * Common trait for grid area points
  * @author Mikko Hilpinen
  * @since 2.11.2023, v1.0.1
  */
trait GridPointOps[+V <: DoubleVectorLike[V], +Repr]
	extends WorldPointOps[V, WorldPoint[HasDoubleDimensions], DoubleVector, Repr] with EqualsBy
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The grid system used to transform coordinates
	  */
	implicit def grid: GridArea
	
	/**
	  * @param surfaceLocation The new surface location to assign
	  * @return Copy of this point at the specified location
	  */
	protected def at(surfaceLocation: HasDoubleDimensions): Repr
	/**
	  * @param latLong The new surface location to assign
	  * @return Copy of this point at the specified location
	  */
	protected def at(latLong: LatLong): Repr
	
	
	// IMPLEMENTED  ------------------
	
	override protected def equalsProperties: Iterable[Any] = Vector(vector, grid)
	
	override def linearDistanceFrom(other: WorldPoint[HasDoubleDimensions]): Distance =
		grid.distanceOf((vector - other.vector).length)
	override def arcingDistanceFrom(other: WorldPoint[HasDoubleDimensions]): Distance =
		linearDistanceFrom(other)
	
	override def +(travel: CompassTravel): Repr = at(vector + travel.axis(grid.vectorLengthOf(travel.distance)))
	override def +(rotation: CompassRotation): Repr = at(latLong + rotation)
	override def +(other: LatLongRotation): Repr = at(latLong + other)
	override def +(vectorTravel: DoubleVector): Repr = at(vector + vectorTravel)
}

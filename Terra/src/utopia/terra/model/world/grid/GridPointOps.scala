package utopia.terra.model.world.grid

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.terra.controller.coordinate.world.{GridArea, VectorDistanceConversion}
import utopia.terra.model.CompassTravel
import utopia.terra.model.world.WorldPoint

/**
  * Common trait for grid area points
  * @author Mikko Hilpinen
  * @since 2.11.2023, v1.0.1
  */
trait GridPointOps[+V <: DoubleVectorLike[V], +Repr]
	extends DoubleWorldPointOps[V, WorldPoint[HasDoubleDimensions], Repr]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The grid system used to transform coordinates
	  */
	implicit def grid: GridArea
	
	
	// IMPLEMENTED  ------------------
	
	override protected def worldView: VectorDistanceConversion = grid
	
	override def arcingDistanceFrom(other: WorldPoint[HasDoubleDimensions]): Distance =
		linearDistanceFrom(other)
	override def +(travel: CompassTravel): Repr = at(vector + travel.axis(worldView.vectorLengthOf(travel.distance)))
}

package utopia.terra.model.world.circle

import utopia.paradigm.measurement.Distance
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.terra.controller.coordinate.world.CircleOfEarth
import utopia.terra.model.world.WorldPoint
import utopia.terra.model.world.grid.DoubleWorldPointOps
/**
  * Common trait for Circle of Earth -world points that specify mathematical equations
  * @author Mikko Hilpinen
  * @since 7.9.2023, v1.0
  */
trait CirclePointOps[V <: DoubleVectorLike[V], +Repr]
	extends DoubleWorldPointOps[V, WorldPoint[HasDoubleDimensions], Repr]
{
	// IMPLICIT --------------------------
	
	override protected implicit def worldView: CircleOfEarth.type = CircleOfEarth
	
	
	// IMPLEMENTED  ----------------------
	
	override def arcingDistanceFrom(other: WorldPoint[HasDoubleDimensions]): Distance = linearDistanceFrom(other)
}

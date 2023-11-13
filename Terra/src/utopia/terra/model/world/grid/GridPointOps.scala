package utopia.terra.model.world.grid

import utopia.paradigm.shape.template.vector.DoubleVectorLike
import utopia.terra.controller.coordinate.world.{GridArea, VectorDistanceConversion}
import utopia.terra.model.CompassTravel
import utopia.terra.model.world.DoubleWorldPointOps

/**
  * Common trait for grid area points
  * @author Mikko Hilpinen
  * @since 2.11.2023, v1.0.1
  * @tparam V Type of vectors used in these points
  * @tparam P Thea actual type of implementing points. Also used in path creation.
  * @tparam Aerial Type of the "aerial" copies of this point
  * @tparam T Type of paths created between these points
  */
trait GridPointOps[+V <: DoubleVectorLike[V], P, +Aerial, +T] extends DoubleWorldPointOps[V, P, Aerial, T]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The grid system used to transform coordinates
	  */
	implicit def grid: GridArea
	
	
	// IMPLEMENTED  ------------------
	
	override protected def worldView: VectorDistanceConversion = grid
	
	override def +(travel: CompassTravel) = at(vector + travel.axis(travel.distance.vectorLength))
}

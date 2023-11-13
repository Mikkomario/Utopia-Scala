package utopia.terra.model.world.grid

import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorLike}
import utopia.terra.controller.coordinate.world.GridArea
import utopia.terra.model.world.Travel

/**
  * Represents travel on a grid-based world view system
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  * @tparam P Type of grid points used
  * @tparam V Type of vector representations used
  */
trait GridTravel[+P <: GridPointOps[V, P, _, _], V <: DoubleVectorLike[V] with DoubleVector]
	extends Travel[Double, P, V, DoubleVector]
{
	// IMPLEMENTED  -----------------
	
	override protected implicit def worldView: GridArea = start.grid
	
	override def arcingDistance = linearDistance
	
	override def apply(progress: Double): P = linearProgress(progress)
	// Arcing travel is not applicable on the grid-system
	override def arcingProgress(progress: Double): P = linearProgress(progress)
}

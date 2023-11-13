package utopia.terra.model.world.grid

import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorLike}
import utopia.terra.controller.coordinate.world.{GridArea, VectorDistanceConversion}
import utopia.terra.model.world.Travel

/**
  * Represents travel on a grid-based world view system
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  * @tparam V Type of vector representations used
  * @tparam P Type of grid points used
  */
trait GridTravel[V <: DoubleVectorLike[V] with DoubleVector, P <: GridPointOps[V, P, _, _]]
	extends Travel[Double, P, V, DoubleVector]
{
	// IMPLEMENTED  -----------------
	
	override protected implicit def worldView: VectorDistanceConversion = GridArea
	
	override def arcingDistance = linearDistance
	
	override def apply(progress: Double): P = linearProgress(progress)
	// Arcing travel is not applicable on the grid-system
	override def arcingProgress(progress: Double): P = linearProgress(progress)
}

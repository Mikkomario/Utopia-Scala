package utopia.terra.model.world.grid

import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorLike}
import utopia.terra.model.world.{Travel, WorldPointOps}

/**
  * Represents travel on a grid-based world view system
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  * @tparam P Type of grid points used
  * @tparam V Type of vector representations used
  */
trait GridTravel[+P <: WorldPointOps[V, _, DoubleVector, P], V <: DoubleVectorLike[V] with DoubleVector]
	extends Travel[Double, P, DoubleVector, V]
{
	// IMPLEMENTED  -----------------
	
	override def arcingDistance = linearDistance
	
	override protected def pointAt(vector: V): P = ???
	
	override def arcingProgress(progress: Double): P = ???
	
	override def apply(progress: Double): P = ???
}

package utopia.terra.model.world.grid

import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.controller.coordinate.world.GridArea

/**
  * Represent 3D travel on a grid-based world system
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  */
case class AerialGridTravel(start: GridPoint, end: GridPoint) extends GridTravel[GridPoint, Vector3D]
{
	override protected def worldView: GridArea = start.grid
}

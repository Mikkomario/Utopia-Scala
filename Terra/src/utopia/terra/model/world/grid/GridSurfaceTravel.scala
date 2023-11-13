package utopia.terra.model.world.grid

import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.terra.model.world.WorldDistance

/**
  * Represents travel on a grid-based world view system
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  * @param start The point where this travel started from
  * @param end The point where this travel ends at
  */
case class GridSurfaceTravel(start: GridSurfacePoint, end: GridSurfacePoint)
	extends GridTravel[Vector2D, GridSurfacePoint]
{
	override def altitudeIncrease: WorldDistance = WorldDistance.zero
}

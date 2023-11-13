package utopia.terra.model.world.circle

import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.terra.model.world.WorldDistance

/**
  * Represents travel between two points on the earth's surface.
  * Assumes the "Circle of Earth" model
  * @author Mikko Hilpinen
  * @since 12.11.2023, v1.1
  */
case class CircleSurfaceTravel(start: CircleSurfacePoint, end: CircleSurfacePoint)
	extends CircleTravel[CircleSurfacePoint, Vector2D]
{
	override def altitudeIncrease: WorldDistance = WorldDistance.zero
	
	override def arcingProgress(progress: Double): CircleSurfacePoint = arcingProgress2D(progress)
}
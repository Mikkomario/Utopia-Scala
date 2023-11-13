package utopia.terra.model.world.grid

import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.model.world.WorldDistance

/**
  * Represent 3D travel on a grid-based world system
  * @author Mikko Hilpinen
  * @since 10.11.2023, v1.1
  */
case class AerialGridTravel(start: AerialGridPoint, end: AerialGridPoint) extends GridTravel[Vector3D, AerialGridPoint]
{
	override def altitudeIncrease: WorldDistance = end.altitude - start.altitude
}
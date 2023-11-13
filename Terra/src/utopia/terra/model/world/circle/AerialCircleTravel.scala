package utopia.terra.model.world.circle

import utopia.paradigm.shape.shape3d.Vector3D
import utopia.terra.model.world.WorldDistance

/**
  * Represents travel between two (aerial) points above the "Circle of Earth"
  * @author Mikko Hilpinen
  * @since 12.11.2023, v1.1
  */
case class AerialCircleTravel(start: AerialCirclePoint, end: AerialCirclePoint) extends CircleTravel[AerialCirclePoint, Vector3D]
{
	override def altitudeIncrease: WorldDistance = end.altitude - start.altitude
	
	override def arcingProgress(progress: Double): AerialCirclePoint = arcingProgress3D(progress) { _.soarBy(_) }
}
package utopia.paradigm.motion.motion3d

import utopia.paradigm.motion.template.MovementHistoryLike
import utopia.paradigm.shape.shape3d.Vector3D

import java.time.Instant

/**
  * Contains recorded position, velocity and acceleration changes over a time period
  * @author Mikko Hilpinen
  * @since Genesis 22.7.2020, v2.3
  */
case class MovementHistory3D(positionHistory: Seq[(Vector3D, Instant)],
                             velocityHistory: Seq[(Velocity3D, Instant)],
                             accelerationHistory: Seq[(Acceleration3D, Instant)])
	extends MovementHistoryLike[Vector3D, Velocity3D, Acceleration3D, MovementStatus3D]
{
	override protected def zeroPosition = Vector3D.zero
	override protected def zeroVelocity = Velocity3D.zero
	override protected def zeroAcceleration = Acceleration3D.zero
	
	override protected def combine(position: Vector3D, velocity: Velocity3D, acceleration: Acceleration3D) =
		MovementStatus3D(position, velocity, acceleration)
}

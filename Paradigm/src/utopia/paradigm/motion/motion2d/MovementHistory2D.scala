package utopia.paradigm.motion.motion2d

import utopia.paradigm.motion.template.MovementHistoryLike
import utopia.paradigm.shape.shape2d.vector.Vector2D

import java.time.Instant

/**
  * Contains recorded position, velocity and acceleration history
  * @author Mikko Hilpinen
  * @since Genesis 22.7.2020, v2.3
  */
case class MovementHistory2D(positionHistory: Vector[(Vector2D, Instant)],
                             velocityHistory: Vector[(Velocity2D, Instant)],
                             accelerationHistory: Vector[(Acceleration2D, Instant)])
	extends MovementHistoryLike[Vector2D, Velocity2D, Acceleration2D, MovementStatus2D]
{
	override protected def zeroPosition = Vector2D.zero
	
	override protected def zeroVelocity = Velocity2D.zero
	
	override protected def zeroAcceleration = Acceleration2D.zero
	
	override protected def combine(position: Vector2D, velocity: Velocity2D, acceleration: Acceleration2D) =
		MovementStatus2D(position, velocity, acceleration)
}

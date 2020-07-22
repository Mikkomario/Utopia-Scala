package utopia.genesis.shape.shape2D

import java.time.Instant

import utopia.genesis.shape.template.MovementHistoryLike

/**
  * Contains recorded position, velocity and acceleration history
  * @author Mikko Hilpinen
  * @since 22.7.2020, v2.3
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

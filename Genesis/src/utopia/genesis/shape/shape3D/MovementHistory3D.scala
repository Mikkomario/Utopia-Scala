package utopia.genesis.shape.shape3D

import java.time.Instant

import utopia.genesis.shape.template.MovementHistoryLike

/**
  * Contains recorded position, velocity and acceleration changes over a time period
  * @author Mikko Hilpinen
  * @since 22.7.2020, v2.3
  */
case class MovementHistory3D(positionHistory: Vector[(Vector3D, Instant)],
							 velocityHistory: Vector[(Velocity3D, Instant)],
							 accelerationHistory: Vector[(Acceleration3D, Instant)])
	extends MovementHistoryLike[Vector3D, Velocity3D, Acceleration3D, MovementStatus3D]
{
	override protected def zeroPosition = Vector3D.zero
	
	override protected def zeroVelocity = Velocity3D.zero
	
	override protected def zeroAcceleration = Acceleration3D.zero
	
	override protected def combine(position: Vector3D, velocity: Velocity3D, acceleration: Acceleration3D) =
		MovementStatus3D(position, velocity, acceleration)
}

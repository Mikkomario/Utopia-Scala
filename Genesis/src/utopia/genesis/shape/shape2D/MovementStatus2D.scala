package utopia.genesis.shape.shape2D

import utopia.genesis.shape.template.MovementStatusLike

/**
  * Combines position, velocity and acceleration data
  * @author Mikko Hilpinen
  * @since 22.7.2020, v2.3
  */
case class MovementStatus2D(position: Vector2D = Vector2D.zero, velocity: Velocity2D = Velocity2D.zero,
							acceleration: Acceleration2D = Acceleration2D.zero)
	extends MovementStatusLike[Vector2D, Velocity2D, Acceleration2D, MovementStatus2D]
{
	override protected def copy(p: Vector2D = position, v: Velocity2D = velocity, a: Acceleration2D = acceleration) =
		MovementStatus2D(p, v, a)
}

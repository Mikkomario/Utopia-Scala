package utopia.genesis.shape.shape3D

import utopia.genesis.shape.template.MovementStatusLike

/**
  * Combines position, velocity and acceleration
  * @author Mikko Hilpinen
  * @since 22.7.2020, v2.3
  */
case class MovementStatus3D(position: Vector3D, velocity: Velocity3D, acceleration: Acceleration3D)
	extends MovementStatusLike[Vector3D, Velocity3D, Acceleration3D, MovementStatus3D]
{
	override protected def copy(p: Vector3D = position, v: Velocity3D = velocity, a: Acceleration3D = acceleration) =
		MovementStatus3D(p, v, a)
}

package utopia.paradigm.motion.motion3d

import utopia.paradigm.motion.template.MovementStatusLike
import utopia.paradigm.shape.shape3d.Vector3D

/**
  * Combines position, velocity and acceleration
  * @author Mikko Hilpinen
  * @since Genesis 22.7.2020, v2.3
  */
case class MovementStatus3D(position: Vector3D, velocity: Velocity3D, acceleration: Acceleration3D)
	extends MovementStatusLike[Vector3D, Velocity3D, Acceleration3D, MovementStatus3D]
{
	override protected def copy(p: Vector3D = position, v: Velocity3D = velocity, a: Acceleration3D = acceleration) =
		MovementStatus3D(p, v, a)
}

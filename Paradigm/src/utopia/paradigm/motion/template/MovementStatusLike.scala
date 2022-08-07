package utopia.paradigm.motion.template

import utopia.paradigm.shape.shape2d.Vector2DLike

import scala.concurrent.duration.Duration

/**
  * Represents a snapshot or a projection of position and movement
  * @author Mikko Hilpinen
  * @since Genesis 22.7.2020, v2.3
  */
trait MovementStatusLike[X <: Vector2DLike[X], V <: VelocityLike[X, V], A <: AccelerationLike[X, V, A], +Repr]
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return A position
	  */
	def position: X
	
	/**
	  * @return A velocity
	  */
	def velocity: V
	
	/**
	  * @return An acceleration
	  */
	def acceleration: A
	
	/**
	  * Creates a new movement status
	  * @param p New position (default = current)
	  * @param v New velocity (default = current)
	  * @param a New acceleration (default = current)
	  * @return A copy of this status
	  */
	protected def copy(p: X = position, v: V = velocity, a: A = acceleration): Repr
	
	
	// COMPUTED	-----------------------
	
	/**
	  * A projection of this movement. Expects acceleration to remain static
	  * @param duration Duration of change
	  * @return Movement status after specified duration
	  */
	def after(duration: Duration) =
	{
		val (transition, newVelocity) = velocity(duration, acceleration)
		copy(position + transition, newVelocity)
	}
}

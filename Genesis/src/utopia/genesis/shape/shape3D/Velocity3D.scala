package utopia.genesis.shape.shape3D

import utopia.flow.util.TimeExtensions._
import utopia.genesis.shape.shape1D.LinearVelocity
import utopia.genesis.shape.shape2D.movement
import utopia.genesis.shape.shape2D.movement.Velocity2D
import utopia.genesis.shape.template.{Dimensional, VectorLike, VelocityLike}

import scala.concurrent.duration.{Duration, TimeUnit}

object Velocity3D
{
	/**
	  * A zero velocity
	  */
	val zero = Velocity3D(Vector3D.zero, 1.seconds)
	
	/**
	  * @param amount Distance vector traversed in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new velocity
	  */
	def apply(amount: Vector3D)(implicit timeUnit: TimeUnit): Velocity3D = new Velocity3D(amount, Duration(1, timeUnit))
	
	/**
	  * @param amount Distance vector traversed in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new velocity
	  */
	def apply(amount: Dimensional[Double])(implicit timeUnit: TimeUnit): Velocity3D =
		apply(Vector3D.withDimensions(amount.dimensions))
}

/**
  * Used for tracking speed in 3D space
  * @author Mikko Hilpinen
  * @since 14.7.2020, v2.3
  * @param transition The amount of transition over 'duration'
  * @param duration The duration of change
  */
case class Velocity3D(transition: Vector3D, override val duration: Duration) extends VelocityLike[Vector3D, Velocity3D]
	with ThreeDimensional[LinearVelocity]
{
	// COMPUTED	-----------------
	
	/**
	  * @return A copy of this velocity without z-axis movement
	  */
	def in2D = movement.Velocity2D(transition.in2D, duration)
	
	
	// IMPLEMENTED	-------------
	
	override protected def zeroTransition = Vector3D.zero
	
	override protected def buildCopy(transition: Vector3D, duration: Duration) = copy(transition, duration)
	
	override def repr = this
	
	override def projectedOver[V <: VectorLike[V]](vector: VectorLike[V]) =
		Velocity3D(transition.projectedOver(vector), duration)
	
	
	// OTHER	-----------------
	
	/**
	  * @param time Target duration
	  * @return An acceleration to increase this amount of velocity in specified time
	  */
	def acceleratedIn(time: Duration) = Acceleration3D(this, time)
}

package utopia.genesis.shape.shape2D

import utopia.flow.util.TimeExtensions._
import utopia.genesis.shape.shape1D.LinearVelocity
import utopia.genesis.shape.shape3D.Velocity3D
import utopia.genesis.shape.template.{VectorLike, VelocityLike}

import scala.concurrent.duration.{Duration, TimeUnit}

object Velocity2D
{
	/**
	  * A zero velocity
	  */
	val zero = Velocity2D(Vector2D.zero, 1.seconds)
	
	/**
	  * @param amount Distance vector traversed in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new velocity
	  */
	def apply(amount: Vector2D)(implicit timeUnit: TimeUnit): Velocity2D = new Velocity2D(amount, Duration(1, timeUnit))
}

/**
  * Used for tracking speed in 2D space
  * @author Mikko Hilpinen
  * @since 14.7.2020, v2.3
  * @param transition The amount of transition over 'duration'
  * @param duration The duration of change
  */
case class Velocity2D(transition: Vector2D, override val duration: Duration) extends VelocityLike[Vector2D, Velocity2D]
	with TwoDimensional[LinearVelocity]
{
	// COMPUTED	-----------------
	
	/**
	  * @return A 3D copy of this velocity
	  */
	def in3D = Velocity3D(transition.in3D, duration)
	
	
	// IMPLEMENTED	-------------
	
	override protected def zeroTransition = Vector2D.zero
	
	override protected def buildCopy(transition: Vector2D, duration: Duration) = copy(transition, duration)
	
	override def repr = this
	
	override def projectedOver[V <: VectorLike[V]](vector: VectorLike[V]) =
		Velocity2D(transition.projectedOver(vector), duration)
	
	
	// OTHER	-----------------
	
	/**
	  * @param time Target duration
	  * @return An acceleration to increase this amount of velocity in specified time
	  */
	def acceleratedIn(time: Duration) = Acceleration2D(this, time)
}

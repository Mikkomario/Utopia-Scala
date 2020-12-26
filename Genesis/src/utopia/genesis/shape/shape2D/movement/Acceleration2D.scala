package utopia.genesis.shape.shape2D.movement

import utopia.flow.util.TimeExtensions._
import utopia.genesis.shape.shape1D.LinearAcceleration
import utopia.genesis.shape.shape2D.{TwoDimensional, Vector2D}
import utopia.genesis.shape.shape3D.Acceleration3D
import utopia.genesis.shape.template.{AccelerationLike, VectorLike}

import scala.concurrent.duration.{Duration, TimeUnit}

object Acceleration2D
{
	/**
	  * A zero acceleration
	  */
	val zero = Acceleration2D(Velocity2D.zero, 1.seconds)
	
	/**
	  * @param velocityChange Amount of velocity change in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new acceleration
	  */
	def apply(velocityChange: Vector2D)(implicit timeUnit: TimeUnit): Acceleration2D =
		new Acceleration2D(Velocity2D(velocityChange), Duration(1, timeUnit))
}

/**
  * Represents a change in velocity over a time period
  * @author Mikko Hilpinen
  * @since 14.7.2020, v2.3
  */
case class Acceleration2D(override val amount: Velocity2D, override val duration: Duration) extends
	AccelerationLike[Vector2D, Velocity2D, Acceleration2D] with TwoDimensional[LinearAcceleration]
{
	// COMPUTED	-----------------------
	
	/**
	  * @return A 3D copy of this acceleration
	  */
	def in3D = Acceleration3D(amount.in3D, duration)
	
	
	// IMPLEMENTED	-------------------
	
	override protected def buildCopy(amount: Velocity2D, duration: Duration) = copy(amount, duration)
	
	override def repr = this
	
	override def projectedOver[V <: VectorLike[V]](vector: VectorLike[V]) =
		Acceleration2D(amount.projectedOver(vector), duration)
}

package utopia.genesis.shape.shape3D

import utopia.flow.util.TimeExtensions._
import utopia.genesis.shape.shape1D.LinearAcceleration
import utopia.genesis.shape.shape2D.Acceleration2D
import utopia.genesis.shape.template.{AccelerationLike, VectorLike}

import scala.concurrent.duration.{Duration, TimeUnit}

object Acceleration3D
{
	/**
	  * A zero acceleration
	  */
	val zero = Acceleration3D(Velocity3D.zero, 1.seconds)
	
	/**
	  * @param velocityChange Amount of velocity change in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new acceleration
	  */
	def apply(velocityChange: Vector3D)(implicit timeUnit: TimeUnit): Acceleration3D =
		new Acceleration3D(Velocity3D(velocityChange), Duration(1, timeUnit))
}

/**
  * Represents a change in velocity over a time period
  * @author Mikko Hilpinen
  * @since 14.7.2020, v2.3
  */
case class Acceleration3D(override val amount: Velocity3D, override val duration: Duration) extends
	AccelerationLike[Vector3D, Velocity3D, Acceleration3D] with ThreeDimensional[LinearAcceleration]
{
	/**
	  * @return A 2D copy of this acceleration (where z-acceleration is 0)
	  */
	def in2D = Acceleration2D(amount.in2D, duration)
	
	
	// IMPLEMENTED	-------------------
	
	override protected def buildCopy(amount: Velocity3D, duration: Duration) = copy(amount, duration)
	
	override def repr = this
	
	override def projectedOver[V <: VectorLike[V]](vector: VectorLike[V]) =
		Acceleration3D(amount.projectedOver(vector), duration)
}

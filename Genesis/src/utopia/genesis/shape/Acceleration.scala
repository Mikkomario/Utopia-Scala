package utopia.genesis.shape

import utopia.genesis.util.Arithmetic
import utopia.flow.util.TimeExtensions._

import scala.concurrent.duration.{Duration, TimeUnit}

object Acceleration
{
	/**
	  * A zero acceleration
	  */
	val zero = Acceleration(Velocity.zero, 1.seconds)
	
	/**
	  * @param velocityChange Amount of velocity change in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new acceleration
	  */
	def apply(velocityChange: Vector3D)(implicit timeUnit: TimeUnit): Acceleration =
		new Acceleration(Velocity(velocityChange), Duration(1, timeUnit))
}

/**
  * Represents a change in velocity over a time period
  * @author Mikko Hilpinen
  * @since 13.9.2019, v2.1+
  */
case class Acceleration(override val amount: Velocity, override val duration: Duration) extends
	Change[Velocity, Acceleration] with Arithmetic[Acceleration, Acceleration] with Dimensional[LinearAcceleration]
	with VectorProjectable[Acceleration]
{
	// COMPUTED	-----------------------
	
	/**
	  * @return Whether this acceleration doesn't actually affect velocity in any way (is zero acceleration)
	  */
	def isZero = amount.isZero
	
	/**
	  * @return Direction of this acceleration
	  */
	def direction = amount.direction
	
	/**
	  * @return A 2D copy of this acceleration (where z-acceleration is 0)
	  */
	def in2D = if (amount.transition.z == 0) this else copy(amount = amount.in2D)
	
	/**
	  * @return This acceleration without the direction component
	  */
	def linear = LinearAcceleration(amount.linear, duration)
	
	
	// IMPLEMENTED	-------------------
	
	override def -(another: Acceleration) = this + (-another)
	
	override def *(mod: Double) = Acceleration(amount * mod, duration)
	
	override def +(another: Acceleration) = Acceleration(amount + another(duration), duration)
	
	override def toString = s"${perMilliSecond.transition}/ms^2"
	
	override def along(axis: Axis) = LinearAcceleration(amount.along(axis), duration)
	
	override def projectedOver(vector: Vector3D) = Acceleration(amount.projectedOver(vector), duration)
}

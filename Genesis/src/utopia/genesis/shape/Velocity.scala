package utopia.genesis.shape

import scala.concurrent.duration.{Duration, TimeUnit}
import utopia.genesis.util.Arithmetic
import utopia.flow.util.TimeExtensions._

object Velocity
{
	/**
	  * A zero velocity
	  */
	val zero = Velocity(Vector3D.zero, 1.seconds)
	
	/**
	  * @param amount Distance vector traversed in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new velocity
	  */
	def apply(amount: Vector3D)(implicit timeUnit: TimeUnit): Velocity = new Velocity(amount, Duration(1, timeUnit))
}

/**
  * Used for tracking speed
  * @author Mikko Hilpinen
  * @since 11.9.2019, v2.1+
  * @param transition The amount of transition over 'duration'
  * @param duration The duration of change
  */
case class Velocity(transition: Vector3D, override val duration: Duration) extends Change[Vector3D, Velocity]
	with Arithmetic[Velocity, Velocity] with Dimensional[LinearVelocity] with VectorProjectable[Velocity]
{
	// ATTRIBUTES	-------------
	
	/**
	  * @return A linear copy of this velocity, based on transition amount / length
	  */
	lazy val linear = LinearVelocity(transition.length, duration)
	
	
	// COMPUTED	-----------------
	
	/**
	  * @return Direction of this velocity vector
	  */
	def direction = transition.direction
	
	/**
	  * @return A copy of this velocity without z-axis movement
	  */
	def in2D = if (transition.z == 0) this else copy(transition = transition.in2D)
	
	/**
	  * @return Whether this velocity is actually stationary (zero)
	  */
	def isZero = transition.isZero
	
	
	// IMPLEMENTED	-------------
	
	override def amount = transition
	
	override def *(mod: Double) = Velocity(transition * mod, duration)
	
	override def +(other: Velocity) = Velocity(transition + other(duration), duration)
	
	override def -(other: Velocity) = this + (-other)
	
	override def toString = s"$perMilliSecond/ms"
	
	def along(axis: Axis) = LinearVelocity(transition.along(axis), duration)
	
	override def projectedOver(vector: Vector3D) = Velocity(transition.projectedOver(vector), duration)
	
	
	// OPERATORS	-------------
	
	def +(other: LinearVelocity) = Velocity(transition + other(duration), duration)
	
	def -(other: LinearVelocity) = this + (-other)
	
	
	// OTHER	-----------------
	
	/**
	  * @param time Target duration
	  * @return An acceleration to increase this amount of velocity in specified time
	  */
	def acceleratedIn(time: Duration) = Acceleration(this, time)
	
	/**
	  * @param amount Amount to increase this velocity
	  * @return A copy of this velocity with increased amount (if provided amount is positive). Direction of this
	  *         velocity is always kept as is, if this velocity was to change direction, a zero velocity is returned
	  *         instead
	  */
	def increasePreservingDirection(amount: LinearVelocity) = if (amount.isPositive || linear > -amount)
		Velocity(transition + amount(duration), duration) else Velocity(Vector3D.zero, duration)
	
	/**
	  * @param amount Amount to decrease this velocity
	  * @return A copy of this velocity with decreased amount (if provided amount is positive). Direction of this
	  *         velocity is always kept as is, if this velocity was to change direction, a zero velocity is returned
	  *         instead
	  */
	def decreasePreservingDirection(amount: LinearVelocity) = increasePreservingDirection(-amount)
	
	/**
	  * Checks the time it takes for this velocity to come to a halt with specified acceleration
	  * @param acceleration Acceleration that would be applied consistently
	  * @return Time it takes for this velocity to reach 0. None if this never happens.
	  */
	def durationUntilStopWith(acceleration: LinearAcceleration) = linear.durationUntilStopWith(acceleration)
	
	/**
	  * Calculates the amount of transition over a period of time when a consistent acceleration is also applied
	  * @param time Transition time
	  * @param acceleration Amount of acceleration (considered to be consistent)
	  * @return The amount of transition in provided time, and also the velocity at the end of that time
	  */
	def apply(time: Duration, acceleration: Acceleration): (Vector3D, Velocity) =
	{
		val endVelocity = this + acceleration(time)
		val averageVelocity = average(endVelocity)
		averageVelocity(time) -> endVelocity
	}
	
	/**
	  * Calculates the amount of transition over a period of time when a consistent acceleration is also applied
	  * @param time Transition time
	  * @param acceleration Amount of acceleration (considered to be consistent)
	  * @param preserveDirection Whether this velocity's direction should be preserved. If true, when this velocity would
	  *                          change directions (by being decreased below 0), a zero velocity will be returned instead
	  * @return The amount of transition in provided time, and also the velocity at the end of that time
	  */
	def apply(time: Duration, acceleration: LinearAcceleration, preserveDirection: Boolean = false): (Vector3D, Velocity) =
	{
		// Sometimes translation & acceleration needs to be stopped when velocity would change direction
		val durationLimit =
		{
			if (preserveDirection)
				durationUntilStopWith(acceleration).filter(_ < time)
			else
				None
		}
		
		durationLimit match
		{
			case Some(limit) => apply(limit, acceleration)
			case None =>
				val endVelocity = this + acceleration(time)
				val averageVelocity = average(endVelocity)
				averageVelocity(time) -> endVelocity
		}
	}
}

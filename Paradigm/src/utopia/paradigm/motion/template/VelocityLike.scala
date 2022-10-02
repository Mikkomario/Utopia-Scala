package utopia.paradigm.motion.template

import utopia.flow.operator.{ApproximatelyZeroable, Combinable, LinearScalable}
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.motion.motion1d.{LinearAcceleration, LinearVelocity}
import utopia.paradigm.shape.shape2d.Vector2DLike
import utopia.paradigm.shape.template.{Dimensional, VectorProjectable}

import scala.concurrent.duration.Duration

/**
  * Used for tracking speed
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  */
trait VelocityLike[Transition <: Vector2DLike[Transition], +Repr <: Change[Transition, Repr]]
	extends Change[Transition, Repr] with LinearScalable[Repr] with Combinable[Repr, Change[Dimensional[Double], _]]
		with ApproximatelyZeroable[Change[Dimensional[Double], _], Repr] with Dimensional[LinearVelocity]
		with VectorProjectable[Repr]
{
	// ABSTRACT	-----------------
	
	def transition: Transition
	
	protected def zeroTransition: Transition
	
	/**
	  * Creates a new copy of this instance
	  * @param transition New transition (default = current)
	  * @param duration   New Duration (default = current)
	  * @return A new copy of this velocity
	  */
	protected def buildCopy(transition: Transition = transition, duration: Duration = duration): Repr
	
	
	// COMPUTED	-----------------
	
	/**
	  * @return A linear copy of this velocity, based on transition amount / length
	  */
	def linear = LinearVelocity(transition.length, duration)
	
	override def zeroDimension = LinearVelocity.zero
	
	/**
	  * @return Direction of this velocity vector
	  */
	def direction = transition.direction
	
	
	// IMPLEMENTED	-------------
	
	override def dimensions = transition.dimensions.map { LinearVelocity(_, duration) }
	
	/**
	  * @return Whether this velocity is actually stationary (zero)
	  */
	override def isZero = transition.isZero || duration.isInfinite
	override def isAboutZero = transition.isAboutZero || duration.isInfinite
	
	override def ~==(other: Change[Dimensional[Double], _]) = perMilliSecond ~== other.perMilliSecond
	
	override def amount = transition
	
	override def *(mod: Double) = buildCopy(transition * mod)
	
	override def +(other: Change[Dimensional[Double], _]) = buildCopy(transition + other(duration))
	
	def -(other: Change[Dimensional[Double], _]) = buildCopy(transition - other(duration))
	
	override def toString = s"$perMilliSecond/ms"
	
	
	// OPERATORS	-------------
	
	def +(other: LinearVelocity) = buildCopy(transition + other(duration))
	
	def -(other: LinearVelocity) = this + (-other)
	
	
	// OTHER	-----------------
	
	/*
	  * @param time Target duration
	  * @return An acceleration to increase this amount of velocity in specified time
	  */
	// TODO: Add? (maybe separate trait)
	// def acceleratedIn(time: Duration) = Acceleration(this, time)
	
	/**
	  * @param amount Amount to increase this velocity
	  * @return A copy of this velocity with increased amount (if provided amount is positive). Direction of this
	  *         velocity is always kept as is, if this velocity was to change direction, a zero velocity is returned
	  *         instead
	  */
	def increasePreservingDirection(amount: LinearVelocity) = if (amount.isPositive || linear > -amount)
		buildCopy(transition + amount(duration)) else buildCopy(zeroTransition, duration)
	
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
	  * @param time         Transition time
	  * @param acceleration Amount of acceleration (considered to be consistent)
	  * @return The amount of transition in provided time, and also the velocity at the end of that time
	  */
	def apply(time: Duration, acceleration: Change[Change[Dimensional[Double], _], _]): (Transition, Repr) =
	{
		val endVelocity = this + acceleration(time)
		val averageVelocity = this.average(endVelocity)
		averageVelocity(time) -> endVelocity
	}
	
	/**
	  * Calculates the amount of transition over a period of time when a consistent acceleration is also applied
	  * @param time              Transition time
	  * @param acceleration      Amount of acceleration (considered to be consistent)
	  * @param preserveDirection Whether this velocity's direction should be preserved. If true, when this velocity would
	  *                          change directions (by being decreased below 0), a zero velocity will be returned instead
	  * @return The amount of transition in provided time, and also the velocity at the end of that time
	  */
	def apply(time: Duration, acceleration: LinearAcceleration, preserveDirection: Boolean = false): (Transition, Repr) =
	{
		// Sometimes translation & acceleration needs to be stopped when velocity would change direction
		val durationLimit = {
			if (preserveDirection)
				durationUntilStopWith(acceleration).filter(_ < time)
			else
				None
		}
		
		durationLimit match {
			case Some(limit) => apply(limit, acceleration)
			case None =>
				val endVelocity = this + acceleration(time)
				val averageVelocity = this.average(endVelocity)
				averageVelocity(time) -> endVelocity
		}
	}
}

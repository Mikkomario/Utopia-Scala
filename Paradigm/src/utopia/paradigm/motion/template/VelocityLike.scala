package utopia.paradigm.motion.template

import utopia.flow.operator.{CanBeAboutZero, Combinable, LinearScalable}
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.motion.motion1d.{LinearAcceleration, LinearVelocity}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.{Dimensional, DoubleVectorLike, VectorProjectable}

import scala.concurrent.duration.Duration

/**
  * Used for tracking speed
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  * @tparam X Type of transition / position
  * @tparam Repr Concrete velocity type
  */
trait VelocityLike[X <: DoubleVectorLike[X], +Repr <: Change[X, Repr]]
	extends Change[X, Repr] with LinearScalable[Repr] with Combinable[Change[HasDoubleDimensions, _], Repr]
		with CanBeAboutZero[Change[HasDoubleDimensions, _], Repr] with Dimensional[LinearVelocity, Repr]
		with VectorProjectable[Repr]
{
	// ABSTRACT	-----------------
	
	/**
	  * @return The amount of transition within the duration of this instance
	  */
	def transition: X
	
	/**
	  * Creates a new copy of this instance
	  * @param transition New transition (default = current)
	  * @param duration   New Duration (default = current)
	  * @return A new copy of this velocity
	  */
	protected def buildCopy(transition: X = transition, duration: Duration = duration): Repr
	
	
	// COMPUTED	-----------------
	
	/**
	  * @return A zero transition
	  */
	def zeroTransition = transition.zero
	
	/**
	  * @return A linear copy of this velocity, based on transition amount / length
	  */
	def linear = LinearVelocity(transition.length, duration)
	
	/**
	  * @return Direction of this velocity vector
	  */
	def direction = transition.direction
	
	
	// IMPLEMENTED	-------------
	
	override def amount = transition
	override def dimensions = transition.dimensions.map { LinearVelocity(_, duration) }
	
	/**
	  * @return Whether this velocity is actually stationary (zero)
	  */
	override def isZero = transition.isZero || duration.isInfinite
	override def isAboutZero = transition.isAboutZero || duration.isInfinite
	
	override def toString = s"$perMilliSecond/ms"
	
	override def ~==(other: Change[HasDoubleDimensions, _]) = perMilliSecond ~== other.perMilliSecond
	
	override def *(mod: Double) = buildCopy(transition * mod)
	override def +(other: Change[HasDoubleDimensions, _]) = buildCopy(transition + other(duration))
	def -(other: Change[HasDoubleDimensions, _]) = buildCopy(transition - other(duration))
	
	
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
	  * @return A copy of this velocity with increased amount.
	  *         Direction of this velocity is always kept as is, if this velocity was to change direction,
	  *         a zero velocity is returned instead
	  */
	def increasePreservingDirection(amount: LinearVelocity) = {
		if (amount.isZero)
			self
		else if (isZero) {
			if (amount.sign.isPositive)
				buildCopy(transition + amount(duration))
			else
				self
		}
		else if (amount.sign.isPositive || linear > -amount)
			buildCopy(transition + amount(duration))
		else
			buildCopy(zeroTransition, duration)
	}
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
	def apply(time: Duration, acceleration: Change[Change[HasDoubleDimensions, _], _]): (X, Repr) = {
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
	def apply(time: Duration, acceleration: LinearAcceleration, preserveDirection: Boolean = false): (X, Repr) = {
		// Sometimes translation & acceleration needs to be stopped when velocity would change direction
		val durationLimit = {
			if (preserveDirection)
				durationUntilStopWith(acceleration).finite.filter(_ < time)
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

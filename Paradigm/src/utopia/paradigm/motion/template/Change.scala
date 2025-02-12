package utopia.paradigm.motion.template

import utopia.flow.time.TimeExtensions._
import utopia.paradigm.transform.LinearSizeAdjustable

import scala.concurrent.duration.Duration

/**
  * Used for describing a change over time
  * @author Mikko Hilpinen
  * @since Genesis 11.9.2019, v2.1+
  * @tparam A    Type of change amount over a specific time period
  * @tparam Repr A concrete implementation of this trait
  */
trait Change[+A, +Repr <: Change[A, _]] extends LinearSizeAdjustable[Repr]
{
	// ABSTRACT	-----------------
	
	/**
	  * @return The amount of change within 'duration'
	  */
	def amount: A
	/**
	  * @return The duration over which change is applied
	  */
	def duration: Duration
	
	
	// COMPUTED	-----------------
	
	/**
	  * @return Change per millisecond
	  */
	def perMilliSecond = apply(1.millis)
	
	
	// OPERATORS	--------------
	
	/**
	  * @param another Another change
	  * @return Whether the effective amount of these changes is equal
	  */
	def ==(another: Change[_, _]) = perMilliSecond == another.perMilliSecond
	
	
	// OTHER	------------------
	
	/**
	  * Same as calling over(Duration)
	  * @param duration Amount of time passed
	  * @return Amount of change in specified time period
	  */
	def apply(duration: Duration) = over(duration)
	
	/**
	  * @param duration Amount of time passed
	  * @return Amount of change over specified time period
	  */
	def over(duration: Duration) = in(duration).amount
	
	/**
	  * @param duration New duration
	  * @return A copy of this change in specified duration. Will have same relative change.
	  */
	def in(duration: Duration) = {
		if (this.duration == Duration.Zero)
			throw new IllegalStateException("Can't calculate distance for changes with zero duration")
		this * (duration / this.duration)
	}
}

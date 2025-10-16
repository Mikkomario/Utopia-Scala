package utopia.paradigm.motion.template

import utopia.flow.operator.MayBeAboutZero
import utopia.flow.time.{Duration, TimeUnit}

/**
 * Common trait for factories used for constructing accelerations
 * @author Mikko Hilpinen
 * @since 16.10.2025, v1.8
 */
trait AccelerationFactory[+A, V <: MayBeAboutZero[V, _], D <: MayBeAboutZero[D, _]] extends ChangeFactory[A, V]
{
	// ABSTRACT ---------------------------
	
	/**
	 * @return Factory used for constructing velocity instances
	 */
	protected def velocityFactory: ChangeFactory[V, D]
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param velocityChange Amount of velocity change in 1 time unit
	 * @param timeUnit Time unit used (implicit)
	 * @return A new acceleration
	 */
	def ofSquare(velocityChange: D)(implicit timeUnit: TimeUnit): A = of(velocityFactory.of(velocityChange))
	
	/**
	 * @param velocityChange Amount of velocity change in 1 time unit
	 * @param unit Time unit used
	 * @return A new acceleration
	 */
	def square(velocityChange: D, unit: TimeUnit): A = apply(velocityFactory(velocityChange, unit), unit)
	/**
	 * @param velocityChange Amount of velocity change within 'duration'
	 * @param duration Duration during which this velocity change occurs
	 * @return A new acceleration affecting velocity by the specified amount over the specified duration
	 */
	def square(velocityChange: D, duration: Duration): A = apply(velocityFactory(velocityChange, duration), duration)
}

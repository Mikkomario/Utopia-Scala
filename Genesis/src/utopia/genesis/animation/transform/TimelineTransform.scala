package utopia.genesis.animation.transform

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * A chain of animations performed over a certain time period
  * @author Mikko Hilpinen
  * @since 11.8.2019, v2.1+
  */
case class TimelineTransform[-Origin, +Reflection](events: Seq[TimedTransform[Origin, Reflection]],
												   delay: FiniteDuration = Duration.Zero)
	extends TimedTransform[Origin, Option[Reflection]]
{
	// ATTRIBUTES	----------------------
	
	/**
	  * The duration of the active portion of this timeline
	  */
	lazy val activeDuration = events.foldLeft[Duration](Duration.Zero) { (total, event) => total + event.duration }
	
	
	// COMPUTED	--------------------------
	
	/**
	  * The total duration of this timeline
	  */
	def duration = delay + activeDuration
	
	
	// OTHER	--------------------------
	
	override def apply(original: Origin, progress: Double): Option[Reflection] = apply(original, duration * progress)
	
	/**
	  * Transforms an item based on the amount of passed time
	  * @param original Original item
	  * @param passedTime Amount of passed time
	  * @return Transformed item. None if there is no active transformation for the specified time
	  */
	override def apply(original: Origin, passedTime: Duration) =
	{
		if (events.isEmpty)
			None
		else if (contains(passedTime))
		{
			// Finds the current event and uses that to transform the original item
			var timeLeft = passedTime - delay
			events.find { event =>
				if (timeLeft <= event.duration)
					true
				else
				{
					timeLeft -= event.duration
					false
				}
				
			}.map { event => event(original, timeLeft / event.duration) }
		}
		else
			None
	}
	
	/**
	  * Whether this timeline is active during the specified timestamp
	  * @param timestamp A timestamp
	  * @return Whether this timeline is active at the time
	  */
	def contains(timestamp: Duration) = timestamp > delay && timestamp <= duration
	
	/**
	  * @param newDelay New delay for this timeline
	  * @return A copy of this timeline with the new delay
	  */
	def withDelay(newDelay: FiniteDuration) = copy(delay = newDelay)
	
	/**
	  * @param delayChange Amount of duration the delay of this timeline is altered
	  * @return A copy of this timeline with altered delay
	  */
	def alterDelay(delayChange: FiniteDuration) = withDelay(delay + delayChange)
}

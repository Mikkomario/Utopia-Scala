package utopia.flow.event.model

import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.View

import java.time.Instant
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
 * Represents some advancement in progress over time
 * @param previousProgress Completion at the last event [0,1]
 * @param currentProgress Completion now [0,1]
 * @param value The current value
 * @param processTime The duration of this process so far
 * @param timestamp Timestamp of this event (default = now)
 * @tparam A Type of wrapped process values
 * @author Mikko Hilpinen
 * @since 07.05.2024, v2.4
 */
case class ProgressEvent[+A](previousProgress: Double, currentProgress: Double, value: A,
                             processTime: FiniteDuration, timestamp: Instant = Now)
	extends View[A]
{
	// ATTRIBUTES   --------------------
	
	/**
	 * Time when the process is projected to complete, based on the current progress and progress time so far.
	 * None if the progress can't be projected yet (at 0% completion).
	 */
	lazy val projectedCompletion = {
		if (currentProgress >= 1.0)
			Some(timestamp)
		else if (currentProgress <= 0 || processTime <= Duration.Zero)
			None
		else {
			val currentProgressPerSecond = currentProgress / processTime.toPreciseSeconds
			val remainingSeconds = (1 - currentProgress) * currentProgressPerSecond
			Some(timestamp + remainingSeconds.seconds)
		}
	}
	
	
	// COMPUTED ------------------------
	
	/**
	 * @return How much the process advanced since the last event [0,1]
	 */
	def advancement = currentProgress - previousProgress
}

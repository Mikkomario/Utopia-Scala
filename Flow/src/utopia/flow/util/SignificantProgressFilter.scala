package utopia.flow.util

import utopia.flow.event.listener.ProgressListener
import utopia.flow.event.model.ProgressEvent

/**
 * A filter that may be applied to progress events, which will reduce and relay the events
 * so that only significant progress is forwarded
 * @tparam A Type of forwarded process values
 * @author Mikko Hilpinen
 * @since 07.05.2024, v2.4
 */
class SignificantProgressFilter[A](significantProgressThreshold: Double, listener: ProgressListener[A])
	extends ProgressListener[A]
{
	// ATTRIBUTES   ----------------------
	
	private var lastEventProgress = 0.0
	
	
	// IMPLEMENTED  ----------------------
	
	override def onProgressEvent(event: ProgressEvent[A]): Unit = {
		// Ignores events until a significant enough progress occurs or until the process completes
		if (event.currentProgress >= 1.0 || event.currentProgress >= lastEventProgress + significantProgressThreshold) {
			val newEvent = event.copy(previousProgress = lastEventProgress)
			lastEventProgress = event.currentProgress
			listener.onProgressEvent(newEvent)
		}
	}
}

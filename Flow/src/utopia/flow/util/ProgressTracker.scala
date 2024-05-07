package utopia.flow.util

import utopia.flow.event.listener.ProgressListener
import utopia.flow.event.model.ProgressEvent
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.eventful.EventfulPointer

object ProgressTracker
{
	/**
	 * Creates a new progress tracker
	 * @param initialValue Initial process value
	 * @param progressFrom A function which calculates the overall progress [0,1] from the specified process value.
	 *                     Return value of 1 or higher is considered to represent process completion.
	 * @tparam A Type of process values used
	 * @return A new progress tracker
	 */
	def apply[A](initialValue: A)(progressFrom: A => Double) = new ProgressTracker[A](initialValue)(progressFrom)
	
	/**
	 * Creates a new progress tracker
	 * @param initialValue Initial process value
	 * @param listener A listener immediately assigned to this tracker
	 * @param progressFrom A function which calculates the overall progress [0,1] from the specified process value.
	 *                     Return value of 1 or higher is considered to represent process completion.
	 * @tparam A Type of process values used
	 * @return A new progress tracker
	 */
	def apply[A](initialValue: A, listener: ProgressListener[A])(progressFrom: A => Double): ProgressTracker[A] = {
		val tracker = apply(initialValue)(progressFrom)
		tracker.addListener(listener)
		tracker
	}
}

/**
 * An interface for converting progress into progress events
 * @param initialValue Initial process value
 * @param progressFrom A function which converts the current process value to a progress from 0 to 1, where
 *                     1 represents process completion.
 * @tparam A Type of process values used
 * @author Mikko Hilpinen
 * @since 07.05.2024, v2.4
 */
class ProgressTracker[A](initialValue: A)(progressFrom: A => Double) extends View[A]
{
	// ATTRIBUTES   --------------------------
	
	private val processStarted = Now.toInstant
	
	private val _pointer = EventfulPointer(initialValue)
	/**
	 * Pointer that contains the currently tracked progress
	 */
	val progressPointer = _pointer.mapUntil(progressFrom) { _ >= 1.0 }
	
	private var listeners = Vector[ProgressListener[A]]()
	
	
	// INITIAL CODE -------------------------
	
	progressPointer.addListener { e =>
		if (listeners.nonEmpty) {
			val now = Now.toInstant
			val event = ProgressEvent(e.oldValue, e.newValue, value, now - processStarted, now)
			listeners.foreach { _.onProgressEvent(event) }
		}
	}
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return The current progress [0,1]
	 */
	def progress = progressPointer.value
	
	/**
	 * @return Pointer to the latest process value
	 */
	def pointer = _pointer.readOnly


	// IMPLEMENTED  -------------------------
	
	override def value: A = _pointer.value
	override def valueIterator = _pointer.valueIterator
	override def mapValue[B](f: A => B) = _pointer.mapValue(f)
	
	
	// OTHER    -----------------------------
	
	/**
	 * Adds a new progress listener to receive events from this interface
	 * @param listener Listener to attach
	 */
	def addListener(listener: ProgressListener[A]) = listeners :+= listener
}

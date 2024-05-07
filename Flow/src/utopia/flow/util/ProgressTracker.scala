package utopia.flow.util

import utopia.flow.event.listener.ProgressListener
import utopia.flow.event.model.ProgressEvent
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer

object ProgressTracker
{
	/**
	 * Creates a new progress tracker
	 * @param pointer The process value pointer to wrap
	 * @param progressFrom A function which calculates the overall progress [0,1] from the specified process value.
	 *                     Return value of 1 or higher is considered to represent process completion.
	 * @tparam A Type of process values used
	 * @return A new progress tracker
	 */
	def wrap[A](pointer: EventfulPointer[A])(progressFrom: A => Double) = new ProgressTracker[A](pointer)(progressFrom)
	
	/**
	 * Creates a new progress tracker
	 * @param initialValue Initial process value
	 * @param progressFrom A function which calculates the overall progress [0,1] from the specified process value.
	 *                     Return value of 1 or higher is considered to represent process completion.
	 * @tparam A Type of process values used
	 * @return A new progress tracker
	 */
	def apply[A](initialValue: A)(progressFrom: A => Double) =
		new ProgressTracker[A](EventfulPointer(initialValue))(progressFrom)
	
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
 * @param pointer The wrapped mutable process value pointer
 * @param progressFrom A function which converts the current process value to a progress from 0 to 1, where
 *                     1 represents process completion.
 * @tparam A Type of process values used
 * @author Mikko Hilpinen
 * @since 07.05.2024, v2.4
 */
class ProgressTracker[A](val pointer: EventfulPointer[A])(progressFrom: A => Double) extends Pointer[A]
{
	// ATTRIBUTES   --------------------------
	
	private val processStarted = Now.toInstant
	
	/**
	 * Pointer that contains the currently tracked progress
	 */
	val progressPointer = pointer.mapUntil(progressFrom) { _ >= 1.0 }
	
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


	// IMPLEMENTED  -------------------------
	
	override def value: A = pointer.value
	override def value_=(newValue: A): Unit = pointer.value = newValue
	override def valueIterator = pointer.valueIterator
	override def mapValue[B](f: A => B) = pointer.mapValue(f)
	
	
	// OTHER    -----------------------------
	
	/**
	 * Adds a new progress listener to receive events from this interface
	 * @param listener Listener to attach
	 */
	def addListener(listener: ProgressListener[A]) = listeners :+= listener
}

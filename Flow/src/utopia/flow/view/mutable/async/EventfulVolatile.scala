package utopia.flow.view.mutable.async

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.event.model.{ChangeEvent, Destiny}
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.LoggingPointerFactory
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper}

import scala.util.Try

object EventfulVolatile extends LoggingPointerFactory[EventfulVolatile]
{
	// IMPLEMENTED    ----------------------
	
	/**
	  * @param initialValue Initial value to assign to this pointer
	  * @param log Implicit logging implementation for handling errors thrown by assigned listeners
	  * @tparam A Type of values held within this container
	  * @return A new pointer
	  */
	override def apply[A](initialValue: A)(implicit log: Logger): EventfulVolatile[A] = new _EventfulVolatile[A](initialValue)
	
	
	// NESTED   ----------------------
	
	private class _EventfulVolatile[A](initialValue: A)(implicit log: Logger) extends EventfulVolatile[A]
	{
		// ATTRIBUTES   -----------
		
		@volatile private var _value: A = initialValue
		
		override lazy val readOnly: Changing[A] = ChangingWrapper(this)
		
		
		// IMPLEMENTED  -----------
		
		override def value: A = _value
		override def destiny: Destiny = ForeverFlux
		
		override def toString = s"Volatile.eventful(${ _value })"
		
		override protected def assignWithoutEvents(newValue: A): Unit = _value = newValue
		
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
	}
}

/**
  * Common trait for mutable containers that are safe to use in a multithreaded environment
  * and which generate change events.
  * @author Mikko Hilpinen
  * @since 27.08.2024, v2.5
  */
abstract class EventfulVolatile[A](implicit listenerLogger: Logger)
	extends AbstractChanging[A] with Volatile[A] with EventfulPointer[A]
{
	// ATTRIBUTES   -------------------
	
	/**
	 * Queues the prepared change events, so that they may be fired outside of this volatile's synchronization,
	 * but still in a synchronous manner.
	 */
	private val eventQueue = Volatile.emptySeq[ChangeEvent[A]]
	/**
	 * A flag that is set to true while processing change events.
	 * Prevents duplicate processes from being initiated.
	 */
	private val processingEventsFlag = Volatile.switch
	
	
	// ABSTRACT -----------------------
	
	/**
	  * Assigns a new value to the handled volatile pointer or variable.
	  * Change event -generation is handled outside of this method call.
	  *
	  * This method is always called in a synchronized block.
	  *
	  * @param newValue New value. Will never match the current value.
	  */
	protected def assignWithoutEvents(newValue: A): Unit
	
	
	// IMPLEMENTED  -------------------
	
	override protected def assign(oldValue: A, newValue: A): Seq[() => Unit] = {
		// Case: Value doesn't change => No-op
		if (newValue == oldValue)
			Empty
		// Case: Value changes => Updates the value and prepares to fire a change event
		else {
			assignWithoutEvents(newValue)
			eventQueue :+= ChangeEvent(oldValue, newValue)
			Single[() => Unit](fireQueuedEvents)
		}
	}
	
	
	// OTHER    ---------------------
	
	private def fireQueuedEvents() = {
		// Won't allow multiple parallel processes
		if (processingEventsFlag.set())
			try {
				// Processes the pending events one by one.
				// Each event is fully resolved (including the after-effects) before the next event is fired
				OptionsIterator.continually { eventQueue.pop() }.foreach { event =>
					fireEvent(event).foreach { effect =>
						Try { effect() }.logWithMessage("Failure during a change effect")
					}
				}
			}
			finally { processingEventsFlag.reset() }
	}
}

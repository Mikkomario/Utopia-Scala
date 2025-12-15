package utopia.flow.view.mutable.eventful

import utopia.flow.collection.immutable.Empty
import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.event.model.{AfterEffect, ChangeEvent, Destiny}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.{LoggingPointerFactory, Pointer}
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper}

object EventfulPointer extends LoggingPointerFactory[EventfulPointer]
{
	// IMPLEMENTED    -----------------------
	
	/**
	  * Creates a new mutable pointer
	  * @param initialValue Initial value to assign to this pointer
	  * @param log Implicit logging implementation for handling failures thrown by event listeners
	  * @tparam A Type of values held within this pointer
	  * @return A new pointer
	  */
	override def apply[A](initialValue: A)(implicit log: Logger): EventfulPointer[A] =
		new _EventfulPointer[A](initialValue)
	
	
	// NESTED   -----------------------
	
	private class _EventfulPointer[A](initialValue: A)(implicit log: Logger)
		extends AbstractChanging[A] with EventfulPointer[A]
	{
		// ATTRIBUTES	----------------
		
		private var _value = initialValue
		
		// Caches the read-only view
		override lazy val readOnly = ChangingWrapper(this)
		
		
		// IMPLEMENTED	----------------
		
		override def destiny: Destiny = ForeverFlux
		
		override def value = _value
		def value_=(newValue: A) = {
			val oldValue = _value
			_value = newValue
			if (oldValue != newValue)
				fireEvent(ChangeEvent(oldValue, newValue))
		}
		override def setAndQueueEvent(newValue: A): IterableOnce[AfterEffect] = {
			val oldValue = _value
			_value = newValue
			if (oldValue == newValue)
				Empty
			else
				fireEventEffects(ChangeEvent(oldValue, newValue))
		}
		
		override def toString = s"Pointer(${_value}).eventful"
		
		// Can never stop changing, so listener assignment is not needed either
		override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
	}
}

/**
  * Common trait for changing items (i.e. items that fire change events) that provide a mutable interface
  * @tparam A Type of the values in this pointer
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1
  */
trait EventfulPointer[A] extends Pointer[A] with Changing[A]
{
	/**
	 * Assigns a new value to this pointer.
	 * Doesn't immediately fire change events, but prepares them as after-effects instead.
	 *
	 * This method may be used in situations where this change originates from another pointer,
	 * when the event-processing should be handled by that pointer instead.
	 *
	 * @param newValue New value to assign to this pointer
	 * @return After-effects to trigger for delivering change events.
	 *         These *must* be triggered by the caller.
	 */
	def setAndQueueEvent(newValue: A): IterableOnce[AfterEffect]
}
package utopia.flow.view.mutable.eventful

import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{AbstractChanging, Changing}

object EventfulPointer
{
	// OTHER    -----------------------
	
	/**
	  * @tparam A Type of values stored in this pointer, when defined
	  * @return A new pointer that's currently empty
	  */
	def empty[A]() = apply[Option[A]](None)
	
	/**
	  * Creates a new mutable pointer
	  * @param initialValue Initial value to assign to this pointer
	  * @tparam A Type of values held within this pointer
	  * @return A new pointer
	  */
	def apply[A](initialValue: A): EventfulPointer[A] = new _EventfulPointer[A](initialValue)
	
	
	// NESTED   -----------------------
	
	private class _EventfulPointer[A](initialValue: A) extends AbstractChanging[A] with EventfulPointer[A]
	{
		// ATTRIBUTES	----------------
		
		private var _value = initialValue
		
		// Caches the read-only view
		override lazy val readOnly = super.readOnly
		
		
		// COMPUTED --------------------
		
		/**
		  * A read-only view into this pointer
		  */
		@deprecated("Please switch to using .readOnly instead", "v2.3")
		def view: Changing[A] = readOnly
		
		
		// IMPLEMENTED	----------------
		
		override def destiny: Destiny = ForeverFlux
		
		/**
		  * @return The current value in this mutable
		  */
		override def value = _value
		/**
		  * @param newValue The new value in this mutable
		  */
		def value_=(newValue: A) = {
			val oldValue = _value
			_value = newValue
			fireEventIfNecessary(oldValue, newValue).foreach { _() }
		}
		
		override def toString = s"EventfulPointer(${_value})"
		
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
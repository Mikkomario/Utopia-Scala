package utopia.flow.view.mutable.eventful

import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper}

object EventfulPointer
{
	/**
	  * @tparam A Type of values stored in this pointer, when defined
	  * @return A new pointer that's currently empty
	  */
	def empty[A]() = new EventfulPointer[Option[A]](None)
	
	/**
	  * Creates a new mutable pointer
	  * @param initialValue Initial value to assign to this pointer
	  * @tparam A Type of values held within this pointer
	  * @return A new pointer
	  */
	def apply[A](initialValue: A) = new EventfulPointer[A](initialValue)
}

/**
  * Classes with this trait generate change events when they mutate
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1
  */
class EventfulPointer[A](initialValue: A) extends AbstractChanging[A] with Pointer[A]
{
	// ATTRIBUTES	----------------
	
	private var _value = initialValue
	
	/**
	 * A read-only view into this pointer
	 */
	lazy val view: Changing[A] = ChangingWrapper(this)
	
	
	// IMPLEMENTED	----------------
	
	override def isChanging = true
	override def mayStopChanging: Boolean = false
	
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
	
	// Can never stop changing, so listener assignment is not needed either
	override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
}
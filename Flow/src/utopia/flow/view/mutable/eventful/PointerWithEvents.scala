package utopia.flow.view.mutable.eventful

import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper}

/**
  * Classes with this trait generate change events when they mutate
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1
  */
class PointerWithEvents[A](initialValue: A) extends AbstractChanging[A] with Pointer[A]
{
	// ATTRIBUTES	----------------
	
	private var _value = initialValue
	
	/**
	 * A read-only view into this pointer
	 */
	lazy val view: Changing[A] = ChangingWrapper(this)
	
	
	// IMPLEMENTED	----------------
	
	override def isChanging = true
	
	/**
	  * @return The current value in this mutable
	  */
	override def value = _value
	/**
	  * @param newValue The new value in this mutable
	  */
	def value_=(newValue: A) =
	{
		if (_value != newValue)
		{
			val oldValue = _value
			_value = newValue
			fireChangeEvent(oldValue)
		}
	}
	
	
	// OTHER	--------------------
	
	@deprecated("Please use .value instead", "v1.9")
	def get = value
	
	@deprecated("Please assign directly to .value instead", "v1.9")
	def set(newVal: A) = value = newVal
}
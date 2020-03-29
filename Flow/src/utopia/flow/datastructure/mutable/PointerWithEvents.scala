package utopia.flow.datastructure.mutable

import utopia.flow.event.Changing

/**
  * Classes with this trait generate change events when they mutate
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1+
  */
class PointerWithEvents[A](initialValue: A) extends PointerLike[A] with Changing[A]
{
	// ATTRIBUTES	----------------
	
	private var _value = initialValue
	
	
	// IMPLEMENTED	----------------
	
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
	
	override def get = value
	
	override def set(newVal: A) = value = newVal
}
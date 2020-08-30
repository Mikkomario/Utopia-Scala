package utopia.flow.datastructure.mutable

import utopia.flow.event.{ChangeListener, Changing}

/**
  * Classes with this trait generate change events when they mutate
  * @author Mikko Hilpinen
  * @since 25.5.2019, v1.4.1+
  */
class PointerWithEvents[A](initialValue: A) extends PointerLike[A] with Changing[A]
{
	// ATTRIBUTES	----------------
	
	private var _value = initialValue
	
	var listeners = Vector[ChangeListener[A]]()
	
	/**
	 * A read-only view into this pointer
	 */
	lazy val view: Changing[A] = new View()
	
	
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
	
	
	// NESTED   --------------------
	
	private class View extends Changing[A]
	{
		override def value = PointerWithEvents.this.value
		
		override def listeners = PointerWithEvents.this.listeners
		
		override def listeners_=(newListeners: Vector[ChangeListener[A]]) =
			PointerWithEvents.this.listeners = newListeners
	}
}
package utopia.reflection.component.input

import utopia.flow.event.{ChangeListener, Changing}

/**
  * This input provides access to a changing element
  * @author Mikko Hilpinen
  * @since 29.6.2019, v1+
  */
trait InputWithPointer[A, +P <: Changing[A]] extends Input[A]
{
	// ABSTRACT	-----------------
	
	/**
	  * @return A pointer to this input's value
	  */
	def valuePointer: P
	
	
	// IMPLEMENTED	-------------
	
	override def value = valuePointer.value
	
	
	// OTHER	-----------------
	
	/**
	  * Registers a new listener to be informed each time this input's value changes
	  * @param listener                        The new listener
	  * @param generateChangeEventFromOldValue None if no change event should be generated for the new listener.
	  *                                        Some with "old" value if a change event should be triggered
	  *                                        <b>for this new listener</b>. Default = None
	  */
	def addValueListener(listener: ChangeListener[A], generateChangeEventFromOldValue: Option[A] = None) =
		valuePointer.addListener(listener, generateChangeEventFromOldValue)
	
	/**
	  * Removes a listener from the informed listeners for value
	  * @param listener A listener
	  */
	def removeValueListener(listener: Any) = valuePointer.removeListener(listener)
}

package utopia.reflection.component.template.input

import utopia.flow.event.listener.ChangeListener
import utopia.flow.view.template.eventful.Changing

/**
  * This input provides access to a changing element
  * @author Mikko Hilpinen
  * @since 29.6.2019, v1+
  */
trait InputWithPointer[+A, +P <: Changing[A]] extends Input[A]
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
	  */
	def addValueListener(listener: ChangeListener[A]) = valuePointer.addListener(listener)
	
	/**
	  * Registers a new listener to be informed each time this input's value changes
	  * @param simulatedValue simulated old value
	  * @param listener                        The new listener
	  */
	def addValueListenerAndSimulateEvent[B >: A](simulatedValue: B)(listener: => ChangeListener[B]) =
		valuePointer.addListenerAndSimulateEvent(simulatedValue)(listener)
	
	/**
	  * Makes sure the specified listener won't be informed anymore
	  * @param listener A listener to no longer inform about value changes
	  */
	def removeValueListener(listener: Any) = valuePointer.removeListener(listener)
}

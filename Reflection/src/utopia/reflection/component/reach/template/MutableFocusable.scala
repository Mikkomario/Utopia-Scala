package utopia.reflection.component.reach.template

import utopia.reflection.event.FocusListener

/**
  * A mutable variation of the focusable trait
  * @author Mikko Hilpinen
  * @since 25.10.2020, v2
  */
trait MutableFocusable extends Focusable
{
	// ABSTRACT	-----------------------------------
	
	def focusListeners_=(newListeners: Seq[FocusListener]): Unit
	
	
	// OTHER	-----------------------------------
	
	/**
	  * Adds a new focus listener to this component
	  * @param listener A new listener to inform on focus events
	  */
	def addFocusListener(listener: FocusListener) = focusListeners :+= listener
	
	/**
	  * Removes a focus listener from this component
	  * @param listener A listener to no longer inform of focus events
	  */
	def removeFocusListener(listener: Any) = focusListeners = focusListeners.filterNot { _ == listener }
}

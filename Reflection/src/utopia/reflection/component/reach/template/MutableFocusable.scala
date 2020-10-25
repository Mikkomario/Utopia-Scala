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
	
	/**
	  * Requests a focus gain for this component
	  * @param forceFocusLeave Whether focus should be forced to leave from the current focus owner (default = false)
	  * @param forceFocusEnter Whether focus should be forced to enter this component (default = false)
	  * @return Whether this component received (or is likely to receive) focus
	  */
	def requestFocus(forceFocusLeave: Boolean = false, forceFocusEnter: Boolean = false) =
		parentHierarchy.top.focusManager.moveFocusTo(this, forceFocusLeave, forceFocusEnter)
}

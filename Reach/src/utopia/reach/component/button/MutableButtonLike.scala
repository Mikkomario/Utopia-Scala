package utopia.reach.component.button

import utopia.reach.component.template.{ButtonLike, MutableFocusable}

/**
  * A common trait for mutable button implementations
  * @author Mikko Hilpinen
  * @since 29.10.2020, v0.1
  */
trait MutableButtonLike extends ButtonLike with MutableFocusable
{
	// ABSTRACT	-------------------------------
	
	/**
	  * @return The actions performed each time this button is triggered
	  */
	protected def actions: Seq[() => Unit]
	protected def actions_=(newActions: Seq[() => Unit]): Unit
	
	def enabled_=(newState: Boolean): Unit
	
	
	// OTHER	--------------------------------
	
	/**
	  * Registers a new action to be performed each time this button is triggered
	  * @param action An action to perform when this button is triggered
	  */
	def registerAction(action: => Unit) = actions :+= (() => action)
}

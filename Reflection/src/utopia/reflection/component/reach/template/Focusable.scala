package utopia.reflection.component.reach.template

import utopia.genesis.shape.shape1D.Direction1D
import utopia.genesis.shape.shape1D.Direction1D.Positive
import utopia.reflection.event.FocusListener

/**
  * A common trait for focusable (reach) components
  * @author Mikko Hilpinen
  * @since 21.10.2020, v2
  */
trait Focusable extends ReachComponentLike
{
	// ABSTRACT	--------------------------------
	
	/**
	  * @return Listeners that will be informed of this component's focus changes
	  */
	def focusListeners: Seq[FocusListener]
	
	/**
	  * @return Whether this component currently allows focus gain
	  */
	def allowsFocusEnter: Boolean
	
	/**
	  * @return Whether this component currently allows focus leave
	  */
	def allowsFocusLeave: Boolean
	
	
	// COMPUTED	-------------------------------
	
	/**
	  * @return The focus manager associated with this component
	  */
	def focusManager = parentCanvas.focusManager
	
	
	// OTHER	-------------------------------
	
	/**
	  * Registers this component to the focus manager
	  */
	protected def enableFocusHandling() = focusManager.register(this)
	
	/**
	  * Detaches this component from the focus manager
	  */
	protected def disableFocusHandling() = focusManager.unregister(this)
	
	/**
	  * Connects this component to the focus manager while linked to the main component hierarchy. Detaches from
	  * the focus manager while not linked.
	  */
	protected def enableFocusHandlingWhileLinked() =
	{
		// Updates registration based on link status
		addHierarchyListener { isLinked =>
			if (isLinked)
				enableFocusHandling()
			else
				disableFocusHandling()
		}
		// Performs the initial registration if already linked
		if (parentHierarchy.isLinked)
			enableFocusHandling()
	}
	
	/**
	  * Requests a focus gain for this component
	  * @param forceFocusLeave Whether focus should be forced to leave from the current focus owner (default = false)
	  * @param forceFocusEnter Whether focus should be forced to enter this component (default = false)
	  * @return Whether this component received (or is likely to receive) focus
	  */
	def requestFocus(forceFocusLeave: Boolean = false, forceFocusEnter: Boolean = false) =
		focusManager.moveFocusTo(this, forceFocusLeave, forceFocusEnter)
	
	/**
	  * Moves the focus one step forward (or backward) from this component.
	  * Only moves the focus if this component is the current focus owner
	  * @param direction Direction towards which the focus is moved (default = Positive = forward)
	  * @param forceFocusLeave Whether to force the focus to leave this component without testing its consent.
	  *                        If true, no FocusLeaving events will be generated. Default = false.
	  */
	def yieldFocus(direction: Direction1D = Positive, forceFocusLeave: Boolean = false) =
		focusManager.moveFocusFrom(this, direction, forceFocusLeave)
}

package utopia.reflection.component.reach.template

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
	
	
	// OTHER	-------------------------------
	
	/**
	  * Registers this component to the focus manager
	  */
	protected def register() = parentHierarchy.top.focusManager.register(this)
	
	/**
	  * Detaches this component from the focus manager
	  */
	protected def unregister() = parentHierarchy.top.focusManager.unregister(this)
	
	/**
	  * Connects this component to the focus manager while linked to the main component hierarchy. Detaches from
	  * the focus manager while not linked.
	  */
	protected def registerWhileLinked() =
	{
		// Updates registration based on link status
		addHierarchyListener { isLinked =>
			if (isLinked)
				register()
			else
				unregister()
		}
		// Performs the initial registration if already linked
		if (parentHierarchy.isLinked)
			register()
	}
}

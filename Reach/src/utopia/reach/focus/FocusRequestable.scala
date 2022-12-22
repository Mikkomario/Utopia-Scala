package utopia.reach.focus

import utopia.reach.component.template.{ReachComponentLike, ReachComponentWrapper}

object FocusRequestable
{
	/**
	  * Wraps a Reach component, delegating focus handling to another item.
	  * This is useful in situations where a component wraps a focus requestable sub-component.
	  * @param wrapped A (parent) component to wrap
	  * @param focusTarget The component that will handle focus requests
	  * @return A new component wrapper
	  */
	def delegate(wrapped: ReachComponentLike, focusTarget: FocusRequestable) =
		wrap(wrapped)(focusTarget.requestFocus)
	/**
	  * Wraps a Reach component, adding it focus request function
	  * @param component A component to wrap
	  * @param requestFocus A focus request function
	  * @return A wrapper around that component
	  */
	def wrap(component: ReachComponentLike)
	        (requestFocus: (Boolean, Boolean) => Boolean): ReachComponentLike with FocusRequestable =
		new FocusRequestableWrapper(component)(requestFocus)
	
	
	// NESTED   -------------------------
	
	private class FocusRequestableWrapper(override protected val wrapped: ReachComponentLike)
	                                     (request: (Boolean, Boolean) => Boolean)
		extends ReachComponentWrapper with FocusRequestable
	{
		override def requestFocus(forceFocusLeave: Boolean, forceFocusEnter: Boolean) =
			request(forceFocusLeave, forceFocusEnter)
	}
}

/**
  * A common trait for components / component managers which allow focus requesting
  * @author Mikko Hilpinen
  * @since 9.3.2021, v0.1
  */
trait FocusRequestable
{
	/**
	  * Requests a focus gain for this component
	  * @param forceFocusLeave Whether focus should be forced to leave from the current focus owner (default = false)
	  * @param forceFocusEnter Whether focus should be forced to enter this component (default = false)
	  * @return Whether this component received (or is likely to receive) focus
	  */
	def requestFocus(forceFocusLeave: Boolean = false, forceFocusEnter: Boolean = false): Boolean
}

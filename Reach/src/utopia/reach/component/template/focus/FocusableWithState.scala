package utopia.reach.component.template.focus

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.reach.focus.FocusTracking

/**
  * A common trait for focusable items which keep track of their focus state
  * @author Mikko Hilpinen
  * @since 31.1.2021, v0.1
  */
trait FocusableWithState extends Focusable with FocusTracking
{
	// ABSTRACT ----------------------------------
	
	/**
	  * @return A pointer to this component's current focus state (true when focused, false when not)
	  */
	def focusPointer: Flag
	
	
	// IMPLEMENTED	--------------------------
	
	override def hasFocus = focusPointer.value
	
	
	// OTHER    ----------------------------------
	
	/**
	  * Adds a new keyboard state listener to this component.
	  * The listener will only be informed while this component has focus.
	  * @param onEvent A function called on keyboard state events
	  */
	def addKeyListenerWhileFocused(onEvent: KeyStateEvent => Unit) =
		_addKeyListenerWhileFocused(AcceptAll)(onEvent)
	
	/**
	  * Adds a new keyboard state listener to this component.
	  * The listener will only be informed while this component has focus.
	  * @param filter  A filter applied to keyboard state events before accepting them
	  * @param onEvent A function called on keyboard state events
	  */
	def addFilteredKeyListenerWhileFocused(filter: Filter[KeyStateEvent])(onEvent: KeyStateEvent => Unit) =
		_addKeyListenerWhileFocused(filter)(onEvent)
	
	private def _addKeyListenerWhileFocused(filter: Filter[KeyStateEvent])(onEvent: KeyStateEvent => Unit) = {
		lazy val listener = new FocusKeyListener(filter)(onEvent)
		addHierarchyListener { isAttached =>
			if (isAttached) KeyboardEvents += listener else KeyboardEvents -= listener
		}
	}
	
	
	// NESTED   ----------------------------------
	
	private class FocusKeyListener(override val keyStateEventFilter: Filter[KeyStateEvent])
	                              (onEvent: KeyStateEvent => Unit)
		extends KeyStateListener
	{
		// IMPLEMENTED  --------------------------
		
		override def handleCondition: Flag = focusPointer
		
		override def onKeyState(event: KeyStateEvent) = onEvent(event)
	}
}

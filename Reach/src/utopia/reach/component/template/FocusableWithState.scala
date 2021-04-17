package utopia.reach.component.template

import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.inception.util.Filter
import utopia.reach.focus.FocusTracking

/**
 * A common trait for focusable items which keep track of their focus state
 * @author Mikko Hilpinen
 * @since 31.1.2021, v0.1
 */
trait FocusableWithState extends Focusable with FocusTracking
{
	// OTHER    ----------------------------------
	
	/**
	 * Adds a new keyboard state listener to this component.
	 * The listener will only be informed while this component has focus.
	 * @param onEvent A function called on keyboard state events
	 */
	def addKeyListenerWhileFocused(onEvent: KeyStateEvent => Unit) = _addKeyListenerWhileFocused(None)(onEvent)
	
	/**
	 * Adds a new keyboard state listener to this component.
	 * The listener will only be informed while this component has focus.
	 * @param filter A filter applied to keyboard state events before accepting them
	 * @param onEvent A function called on keyboard state events
	 */
	def addFilteredKeyListenerWhileFocused(filter: Filter[KeyStateEvent])(onEvent: KeyStateEvent => Unit) =
		_addKeyListenerWhileFocused(Some(filter))(onEvent)
	
	private def _addKeyListenerWhileFocused(filter: Option[Filter[KeyStateEvent]])(onEvent: KeyStateEvent => Unit) =
	{
		lazy val listener = new FocusKeyListener(filter)(onEvent)
		addHierarchyListener { isAttached =>
			if (isAttached)
				GlobalKeyboardEventHandler += listener
			else
				GlobalKeyboardEventHandler -= listener
		}
	}
	
	
	// NESTED   ----------------------------------
	
	private class FocusKeyListener(filter: Option[Filter[KeyStateEvent]])(onEvent: KeyStateEvent => Unit)
		extends KeyStateListener
	{
		// ATTRIBUTES   --------------------------
		
		override val keyStateEventFilter = filter.getOrElse(super.keyStateEventFilter)
		
		
		// IMPLEMENTED  --------------------------
		
		override def onKeyState(event: KeyStateEvent) = onEvent(event)
		
		override def allowsHandlingFrom(handlerType: HandlerType) = hasFocus
	}
}

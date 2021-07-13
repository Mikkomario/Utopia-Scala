package utopia.reach.window

import utopia.flow.datastructure.template.Viewable
import utopia.flow.event.AlwaysTrue
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.KeyStateListener
import utopia.genesis.view.GlobalKeyboardEventHandler
import utopia.inception.handling.HandlerType
import utopia.reach.component.template.focus.FocusableWithState
import utopia.reflection.container.swing.window.Window

import java.awt.event.KeyEvent
import scala.concurrent.ExecutionContext

object WindowDefaultButtonKeyTriggerer
{
	/**
	  * Creates a new key triggerer and registers it to handle keyboard events while the window is displayed
	  * @param window Window to which this listener responds / is connected to
	  * @param buttons Buttons / actionable items that prevent this listener from triggering when they have focus
	  * @param additionalCondition A condition which must be met in order for the action to be triggered
	  *                            (default = always enabled)
	  * @param triggerKeyIndex Keyboard index which causes this trigger (default = enter)
	  * @param action Action performed on trigger
	  * @param exc Implicit execution context
	  * @return A new window button listener
	  */
	def register(window: Window[_], buttons: Iterable[FocusableWithState],
			  additionalCondition: Viewable[Boolean] = AlwaysTrue,
			  triggerKeyIndex: Int = KeyEvent.VK_ENTER)(action: => Unit)
			 (implicit exc: ExecutionContext) =
	{
		// Creates the listener
		val listener = new WindowDefaultButtonKeyTriggerer(window, buttons, additionalCondition,
			triggerKeyIndex)(action)
		// Registers the listener to listen to events
		GlobalKeyboardEventHandler += listener
		// When the window closes, automatically removes this component from the keyboard listening interface
		window.closeFuture.foreach { _ => GlobalKeyboardEventHandler -= listener }
	}
}

/**
  * A keyboard listener used for triggering one of a window's buttons
  * @author Mikko Hilpinen
  * @since 4.3.2021, v0.1
  */
class WindowDefaultButtonKeyTriggerer(window: Window[_], buttons: Iterable[FocusableWithState],
									  additionalCondition: Viewable[Boolean] = AlwaysTrue,
									  triggerKeyIndex: Int = KeyEvent.VK_ENTER)(action: => Unit)
	extends KeyStateListener
{
	// ATTRIBUTES	----------------------------
	
	override val keyStateEventFilter = KeyStateEvent.wasPressedFilter && KeyStateEvent.keyFilter(triggerKeyIndex)
	
	
	// IMPLEMENTED	----------------------------
	
	override def onKeyState(event: KeyStateEvent) =
	{
		// Checks whether required conditions are met
		if (buttons.forall { !_.hasFocus } && additionalCondition.value)
			action
	}
	
	override def allowsHandlingFrom(handlerType: HandlerType) =
		window.isFocusedWindow && window.visible
}

package utopia.reach.window

import utopia.firmament.component.Window
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.keyboard.Key.Enter
import utopia.genesis.handling.event.keyboard.{Key, KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.reach.component.template.focus.FocusableWithState

import scala.concurrent.ExecutionContext

object WindowDefaultButtonKeyTriggerer
{
	/**
	  * Creates a new key triggerer and registers it to handle keyboard events while the window is displayed
	  * @param window Window to which this listener responds / is connected to
	  * @param buttons Buttons / actionable items that prevent this listener from triggering when they have focus
	  * @param additionalCondition A condition which must be met in order for the action to be triggered
	  *                            (default = always enabled)
	  * @param triggerKey Key which causes this trigger (default = enter)
	  * @param action Action performed on trigger
	  * @param exc Implicit execution context
	  * @return A new window button listener
	  */
	def register(window: Window, buttons: Iterable[FocusableWithState], additionalCondition: View[Boolean] = AlwaysTrue,
	             triggerKey: Key = Enter)
	            (action: => Unit)
	            (implicit exc: ExecutionContext) =
	{
		// Creates the listener
		val listener = new WindowDefaultButtonKeyTriggerer(window, buttons, additionalCondition, triggerKey)(action)
		// Registers the listener to listen to events
		KeyboardEvents += listener
		// When the window closes, automatically removes this component from the keyboard listening interface
		window.closeFuture.foreach { _ => KeyboardEvents -= listener }
	}
}

/**
  * A keyboard listener used for triggering one of a window's buttons
  * @author Mikko Hilpinen
  * @since 4.3.2021, v0.1
  */
class WindowDefaultButtonKeyTriggerer(window: Window, buttons: Iterable[FocusableWithState],
                                      additionalCondition: View[Boolean] = AlwaysTrue, triggerKey: Key = Enter)
                                     (action: => Unit)
	extends KeyStateListener
{
	// ATTRIBUTES	----------------------------
	
	override val keyStateEventFilter = KeyStateEvent.filter.pressed && KeyStateEvent.filter(triggerKey)
	
	
	// IMPLEMENTED	----------------------------
	
	override def handleCondition: FlagLike = window.fullyVisibleAndFocusedFlag
	
	override def onKeyState(event: KeyStateEvent) = {
		// Checks whether required conditions are met
		if (buttons.forall { !_.hasFocus } && additionalCondition.value)
			action
	}
}

package utopia.reflection.container.swing.window

import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.event.keyboard.Key.Enter
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener}
import utopia.reflection.component.swing.button.ButtonLike

object DefaultButtonHandler
{
	/**
	  * Creates a new button handler
	  * @param defaultButton The default button that will be triggered on enter
	  * @param moreButtons Other buttons in the window
	  * @param triggerCondition A condition for the default button triggering
	  * @return A new button handler
	  */
	def apply(defaultButton: ButtonLike, moreButtons: ButtonLike*)(triggerCondition: => Boolean) =
		new DefaultButtonHandler(defaultButton, defaultButton +: moreButtons)(triggerCondition)
}

/**
  * This instance listens to key events and triggers a default button when no other button is in focus
  * @author Mikko Hilpinen
  * @since 8.5.2019, v1+
  * @param defaultButton The button that will be triggered on enter
  * @param allButtons All buttons in a window
  */
class DefaultButtonHandler(val defaultButton: ButtonLike, val allButtons: Iterable[ButtonLike])
                          (triggerCondition: => Boolean)
	extends KeyStateListener
{
	// Only triggers on enter
	override val keyStateEventFilter = KeyStateEvent.filter.pressed && KeyStateEvent.filter(Enter)
	
	override def handleCondition: FlagLike = AlwaysTrue
	
	override def onKeyState(event: KeyStateEvent) = {
		// Only listens to events while a) none of the buttons is in focus and b) additional focus condition
		if (allButtons.forall {!_.isInFocus } && triggerCondition)
			defaultButton.trigger()
	}
}

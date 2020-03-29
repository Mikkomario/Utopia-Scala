package utopia.reflection.container.swing.window

import java.awt.event.KeyEvent

import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.KeyStateListener
import utopia.inception.handling.HandlerType
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
class DefaultButtonHandler(val defaultButton: ButtonLike, val allButtons: Traversable[ButtonLike])(
	triggerCondition: => Boolean) extends KeyStateListener
{
	override def parent = None
	
	// Only listens to events while a) none of the buttons is in focus and b) additional focus condition
	override def allowsHandlingFrom(handlerType: HandlerType) = allButtons.forall { !_.isInFocus } && triggerCondition
	
	// Only triggers on enter
	override val keyStateEventFilter = KeyStateEvent.wasPressedFilter && KeyStateEvent.keyFilter(KeyEvent.VK_ENTER)
	
	// If no button has focus, triggers the default button
	override def onKeyState(event: KeyStateEvent) = if (allButtons.forall {!_.isInFocus }) defaultButton.trigger()
}

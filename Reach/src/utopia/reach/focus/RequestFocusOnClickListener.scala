package utopia.reach.focus

import utopia.genesis.event.{MouseButtonStateEvent, MouseEvent}
import utopia.genesis.handling.MouseButtonStateListener
import utopia.inception.handling.HandlerType
import utopia.reach.component.template.ReachComponentLike

/**
  * A mouse event listener that moves component to focus when it is clicked
  * @author Mikko Hilpinen
  * @since 30.11.2022, v0.5
  * @param component The component that is brought to focus when it is clicked
  * @param enabled A function that determines whether this feature is enabled or not. Default = always enabled.
  */
class RequestFocusOnClickListener(component: FocusRequestable with ReachComponentLike, enabled: => Boolean = true)
	extends MouseButtonStateListener
{
	// ATTRIBUTES   -----------------------------
	
	override val mouseButtonStateEventFilter =
		MouseButtonStateEvent.leftPressedFilter && MouseEvent.isOverAreaFilter(component.bounds)
	
	
	// IMPLEMENTED  -----------------------------
	
	override def isReceivingMouseButtonStateEvents = enabled
	
	override def onMouseButtonState(event: MouseButtonStateEvent) = {
		component.requestFocus()
		None
	}
	
	override def allowsHandlingFrom(handlerType: HandlerType) = true
}

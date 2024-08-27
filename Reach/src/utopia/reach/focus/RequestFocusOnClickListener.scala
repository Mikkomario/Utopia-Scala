package utopia.reach.focus

import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.Flag
import utopia.genesis.handling.event.consume.ConsumeChoice.Preserve
import utopia.genesis.handling.event.mouse.{MouseButtonStateEvent, MouseButtonStateListener, MouseEvent}
import utopia.reach.component.template.ReachComponentLike

/**
  * A mouse event listener that moves component to focus when it is clicked
  * @author Mikko Hilpinen
  * @since 30.11.2022, v0.5
  * @param component The component that is brought to focus when it is clicked
  * @param enabledFlag A pointer that determines whether this feature is enabled or not. Default = always enabled.
  */
class RequestFocusOnClickListener(component: FocusRequestable with ReachComponentLike,
                                  enabledFlag: Flag = AlwaysTrue)
	extends MouseButtonStateListener
{
	// ATTRIBUTES   -----------------------------
	
	override val mouseButtonStateEventFilter =
		MouseButtonStateEvent.filter.leftPressed && MouseEvent.filter.over(component.bounds)
	
	
	// IMPLEMENTED  -----------------------------
	
	override def handleCondition: Flag = enabledFlag
	
	override def onMouseButtonStateEvent(event: MouseButtonStateEvent) = {
		component.requestFocus()
		Preserve
	}
}

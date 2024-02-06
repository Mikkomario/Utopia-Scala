package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.Filter
import utopia.genesis.handling.event.{ConsumableEventHandler2, ConsumeChoice}
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}

/**
  * A handler used for distributing mouse button state events
  * @author Mikko Hilpinen
  * @since 05/02/2024, v3.6
  */
class MouseButtonStateHandler(initialListeners: IterableOnce[MouseButtonStateListener2] = Vector.empty)
	extends DeepHandler2[MouseButtonStateListener2](initialListeners)
		with ConsumableEventHandler2[MouseButtonStateListener2, MouseButtonStateEvent2]
{
	// IMPLEMENTED  ---------------------
	
	override protected def filterOf(listener: MouseButtonStateListener2): Filter[MouseButtonStateEvent2] =
		listener.mouseButtonStateEventFilter
	override protected def deliver(listener: MouseButtonStateListener2, event: MouseButtonStateEvent2): ConsumeChoice =
		listener.onMouseButtonStateEvent(event)
	
	override protected def asHandleable(item: Handleable2): Option[MouseButtonStateListener2] = item match {
		case l: MouseButtonStateListener2 => Some(l)
		case _ => None
	}
}

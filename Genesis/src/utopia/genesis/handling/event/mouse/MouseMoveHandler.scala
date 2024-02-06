package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.genesis.handling.event.EventHandler2
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}

/**
  * A handler used for distributing mouse moved -events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v3.6
  */
class MouseMoveHandler(initialListeners: IterableOnce[MouseMoveListener2] = Vector.empty)
	extends DeepHandler2[MouseMoveListener2](initialListeners) with EventHandler2[MouseMoveListener2, MouseMoveEvent2]
		with MouseMoveListener2
{
	override def mouseMoveEventFilter: Filter[MouseMoveEvent2] = AcceptAll
	
	override protected def filterOf(listener: MouseMoveListener2): Filter[MouseMoveEvent2] =
		listener.mouseMoveEventFilter
	override protected def deliver(listener: MouseMoveListener2, event: MouseMoveEvent2): Unit =
		listener.onMouseMove(event)
	
	override protected def asHandleable(item: Handleable2): Option[MouseMoveListener2] = item match {
		case l: MouseMoveListener2 => Some(l)
		case _ => None
	}
	
	override def onMouseMove(event: MouseMoveEvent2): Unit = distribute(event)
}

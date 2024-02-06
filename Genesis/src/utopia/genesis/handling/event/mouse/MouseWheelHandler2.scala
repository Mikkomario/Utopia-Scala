package utopia.genesis.handling.event.mouse

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.genesis.handling.event.{ConsumableEventHandler2, ConsumeChoice}
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}

/**
  * A handler used for distributing mouse wheel events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v3.6
  */
class MouseWheelHandler2(initialListeners: IterableOnce[MouseWheelListener2] = Vector.empty)
	extends DeepHandler2[MouseWheelListener2](initialListeners)
		with ConsumableEventHandler2[MouseWheelListener2, MouseWheelEvent2] with MouseWheelListener2
{
	override def mouseWheelEventFilter: Filter[MouseWheelEvent2] = AcceptAll
	
	override protected def filterOf(listener: MouseWheelListener2): Filter[MouseWheelEvent2] =
		listener.mouseWheelEventFilter
	override protected def deliver(listener: MouseWheelListener2, event: MouseWheelEvent2): ConsumeChoice =
		listener.onMouseWheelRotated(event)
	
	override def onMouseWheelRotated(event: MouseWheelEvent2): ConsumeChoice = distribute(event)._2
	
	override protected def asHandleable(item: Handleable2): Option[MouseWheelListener2] = item match {
		case l: MouseWheelListener2 => Some(l)
		case _ => None
	}
}

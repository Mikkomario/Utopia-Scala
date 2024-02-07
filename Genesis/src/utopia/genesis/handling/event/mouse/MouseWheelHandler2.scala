package utopia.genesis.handling.event.mouse

import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.genesis.handling.event.{ConsumableEventHandler2, ConsumeChoice}
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}

object MouseWheelHandler2 extends FromCollectionFactory[MouseWheelListener2, MouseWheelHandler2]
{
	// IMPLEMENTED  ------------------------
	
	override def from(items: IterableOnce[MouseWheelListener2]): MouseWheelHandler2 = apply(items)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param listeners Listeners to place on this handler, initially
	  * @return A handler managing the specified listeners
	  */
	def apply(listeners: IterableOnce[MouseWheelListener2]) = new MouseWheelHandler2(listeners)
}

/**
  * A handler used for distributing mouse wheel events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
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

package utopia.genesis.handling.event.mouse

import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.genesis.handling.event.consume.{ConsumableEventHandler2, ConsumeChoice}
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}

object MouseButtonStateHandler extends FromCollectionFactory[MouseButtonStateListener2, MouseButtonStateHandler]
{
	// IMPLEMENTED  ------------------------
	
	override def from(items: IterableOnce[MouseButtonStateListener2]): MouseButtonStateHandler = apply(items)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param listeners Listeners to place on this handler, initially
	  * @return A handler managing the specified listeners
	  */
	def apply(listeners: IterableOnce[MouseButtonStateListener2]) = new MouseButtonStateHandler(listeners)
}

/**
  * A handler used for distributing mouse button state events
  * @author Mikko Hilpinen
  * @since 05/02/2024, v4.0
  */
class MouseButtonStateHandler(initialListeners: IterableOnce[MouseButtonStateListener2] = Iterable.empty)
	extends DeepHandler2[MouseButtonStateListener2](initialListeners)
		with ConsumableEventHandler2[MouseButtonStateListener2, MouseButtonStateEvent2] with MouseButtonStateListener2
{
	// IMPLEMENTED  ---------------------
	
	override def mouseButtonStateEventFilter: Filter[MouseButtonStateEvent2] = AcceptAll
	
	override protected def filterOf(listener: MouseButtonStateListener2): Filter[MouseButtonStateEvent2] =
		listener.mouseButtonStateEventFilter
	override protected def deliver(listener: MouseButtonStateListener2, event: MouseButtonStateEvent2): ConsumeChoice =
		listener.onMouseButtonStateEvent(event)
	
	override protected def asHandleable(item: Handleable2): Option[MouseButtonStateListener2] = item match {
		case l: MouseButtonStateListener2 => Some(l)
		case _ => None
	}
	
	override def onMouseButtonStateEvent(event: MouseButtonStateEvent2): ConsumeChoice = distribute(event)._2
}

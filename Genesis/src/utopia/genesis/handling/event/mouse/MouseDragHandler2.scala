package utopia.genesis.handling.event.mouse

import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.genesis.handling.event.EventHandler2
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}

object MouseDragHandler2 extends FromCollectionFactory[MouseDragListener2, MouseDragHandler2]
{
	// IMPLEMENTED  ------------------------
	
	override def from(items: IterableOnce[MouseDragListener2]): MouseDragHandler2 = apply(items)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param listeners Listeners to place on this handler, initially
	  * @return A handler managing the specified listeners
	  */
	def apply(listeners: IterableOnce[MouseDragListener2]) = new MouseDragHandler2(listeners)
}

/**
  * A handler used for distributing mouse drag events
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  */
class MouseDragHandler2(initialListeners: IterableOnce[MouseDragListener2] = Vector.empty)
	extends DeepHandler2[MouseDragListener2](initialListeners) with EventHandler2[MouseDragListener2, MouseDragEvent2]
		with MouseDragListener2
{
	override def mouseDragEventFilter: Filter[MouseDragEvent2] = AcceptAll
	
	override protected def filterOf(listener: MouseDragListener2): Filter[MouseDragEvent2] =
		listener.mouseDragEventFilter
	override protected def deliver(listener: MouseDragListener2, event: MouseDragEvent2): Unit =
		listener.onMouseDrag(event)
	
	override def onMouseDrag(event: MouseDragEvent2): Unit = distribute(event)
	
	override protected def asHandleable(item: Handleable2): Option[MouseDragListener2] = item match {
		case l: MouseDragListener2 => Some(l)
		case _ => None
	}
}

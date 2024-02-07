package utopia.genesis.handling.event.keyboard

import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.genesis.handling.event.EventHandler2
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}

object KeyTypedHandler2 extends FromCollectionFactory[KeyTypedListener2, KeyTypedHandler2]
{
	// IMPLEMENTED  --------------------
	
	override def from(items: IterableOnce[KeyTypedListener2]): KeyTypedHandler2 = apply(items)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param listeners Listeners to place on this handler, initially
	  * @return A new handler managing those listeners
	  */
	def apply(listeners: IterableOnce[KeyTypedListener2]) = new KeyTypedHandler2(listeners)
}

/**
  * A handler used for distributing key typed -events
  * @author Mikko Hilpinen
  * @since 05/02/2024, v4.0
  */
class KeyTypedHandler2(initialListeners: IterableOnce[KeyTypedListener2] = Iterable.empty)
	extends DeepHandler2[KeyTypedListener2](initialListeners) with EventHandler2[KeyTypedListener2, KeyTypedEvent2]
		with KeyTypedListener2
{
	override def keyTypedEventFilter: Filter[KeyTypedEvent2] = AcceptAll
	
	override protected def filterOf(listener: KeyTypedListener2): Filter[KeyTypedEvent2] = listener.keyTypedEventFilter
	override protected def deliver(listener: KeyTypedListener2, event: KeyTypedEvent2): Unit =
		listener.onKeyTyped(event)
	
	override def onKeyTyped(event: KeyTypedEvent2): Unit = distribute(event)
	
	override protected def asHandleable(item: Handleable2): Option[KeyTypedListener2] = item match {
		case l: KeyTypedListener2 => Some(l)
		case _ => None
	}
}

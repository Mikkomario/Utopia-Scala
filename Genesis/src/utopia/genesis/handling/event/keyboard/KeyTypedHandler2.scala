package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.genesis.handling.template.{DeepHandler2, EventHandler2, Handleable2}

/**
  * A handler used for distributing key typed -events
  * @author Mikko Hilpinen
  * @since 05/02/2024, v3.6
  */
class KeyTypedHandler2(initialListeners: IterableOnce[KeyTypedListener2])
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

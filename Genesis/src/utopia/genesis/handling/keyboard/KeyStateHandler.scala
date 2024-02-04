package utopia.genesis.handling.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.genesis.event.KeyStateEvent
import utopia.genesis.handling.template.{DeepHandler2, EventHandler2, Handleable2}

/**
  * A handler that distributes key-state events
  * @author Mikko Hilpinen
  * @since 03/02/2024, v3.6
  */
class KeyStateHandler(initialListners: IterableOnce[KeyStateListener2] = Vector.empty)
	extends DeepHandler2[KeyStateListener2](initialListners)
		with EventHandler2[KeyStateListener2, KeyStateEvent] with KeyStateListener2
{
	override def keyStateEventFilter: Filter[KeyStateEvent] = AcceptAll
	
	override def onKeyState(event: KeyStateEvent): Unit = distribute(event)
	
	override protected def filterOf(listener: KeyStateListener2) = listener.keyStateEventFilter
	override protected def deliver(listener: KeyStateListener2, event: KeyStateEvent): Unit =
		listener.onKeyState(event)
	
	override protected def asHandleable(item: Handleable2): Option[KeyStateListener2] = item match {
		case l: KeyStateListener2 => Some(l)
		case _ => None
	}
}

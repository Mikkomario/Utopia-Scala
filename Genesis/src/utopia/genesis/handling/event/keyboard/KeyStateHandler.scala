package utopia.genesis.handling.event.keyboard

import utopia.flow.operator.filter.{AcceptAll, Filter}
import utopia.genesis.handling.event.EventHandler2
import utopia.genesis.handling.template.{DeepHandler2, Handleable2}

/**
  * A handler that distributes key-state events
  * @author Mikko Hilpinen
  * @since 03/02/2024, v3.6
  */
class KeyStateHandler(initialListeners: IterableOnce[KeyStateListener2] = Vector.empty)
	extends DeepHandler2[KeyStateListener2](initialListeners)
		with EventHandler2[KeyStateListener2, KeyStateEvent2] with KeyStateListener2
{
	override def keyStateEventFilter: Filter[KeyStateEvent2] = AcceptAll
	
	override def onKeyState(event: KeyStateEvent2): Unit = distribute(event)
	
	override protected def filterOf(listener: KeyStateListener2) =
		listener.keyStateEventFilter
	override protected def deliver(listener: KeyStateListener2, event: KeyStateEvent2): Unit =
		listener.onKeyState(event)
	
	override protected def asHandleable(item: Handleable2): Option[KeyStateListener2] = item match {
		case l: KeyStateListener2 => Some(l)
		case _ => None
	}
}

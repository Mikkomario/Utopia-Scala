package utopia.genesis.handling

import utopia.genesis.event.{Consumable, ConsumeEvent}
import utopia.inception.handling.Handleable

/**
  * These handlers distribute consumable events
  * @author Mikko Hilpinen
  * @since 10.5.2019, v1+
  */
trait ConsumableEventHandler[Listener <: Handleable, Event <: Consumable[Event]] extends EventHandler[Listener, Event]
{
	// ABSTRACT	---------------------
	
	/**
	  * Informs a listener about an event
	  * @param listener A listener
	  * @param event An event
	  * @return A consume event if the event was consumed during this process
	  */
	override protected def inform(listener: Listener, event: Event): Option[ConsumeEvent]
	
	
	// OTHER	--------------------
	
	override def distribute(event: Event): Option[ConsumeEvent] = event.distributeAmong(handleView().toVector) {
		(l, e) => if (eventFilterFor(l)(e)) inform(l, e) else None
	}
}

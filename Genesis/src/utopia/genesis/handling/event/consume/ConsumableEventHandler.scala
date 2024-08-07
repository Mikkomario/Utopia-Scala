package utopia.genesis.handling.event.consume

import utopia.flow.operator.filter.Filter
import utopia.genesis.handling.event.consume.ConsumeChoice.Preserve
import utopia.genesis.handling.template.{Handleable, Handler}

/**
  * A common trait for event-distributing handlers that deal with consumable events
  * @author Mikko Hilpinen
  * @since 05/02/2024, v4.0
  */
trait ConsumableEventHandler[Listener <: Handleable, Event <: Consumable[Event]] extends Handler[Listener]
{
	// ABSTRACT ------------------------
	
	/**
	  * @param listener A listener
	  * @return An event filter applied by that listener
	  */
	protected def filterOf(listener: Listener): Filter[Event]
	
	/**
	  * Delivers an event to a listener
	  * @param listener A listener to inform of an event
	  * @param event The event to deliver to the listener
	  * @return Consume choice made by the listener
	  */
	protected def deliver(listener: Listener, event: Event): ConsumeChoice
	
	
	// OTHER    -----------------------
	
	/**
	  * Distributes the specified event among all active listeners
	  * @param event The event to distribute
	  * @return The event after distributing (possibly consumed) +
	  *         a consume choice indicating whether the event was consumed or not
	  */
	def distribute(event: Event) = event.distribute(items) { (listener, event) =>
		if (filterOf(listener)(event))
			deliver(listener, event)
		else
			Preserve
	}
}

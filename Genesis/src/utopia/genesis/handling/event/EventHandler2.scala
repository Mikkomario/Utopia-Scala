package utopia.genesis.handling.event

import utopia.flow.operator.filter.Filter
import utopia.genesis.handling.template.{Handleable2, Handler2}

/**
  * Event handlers distribute events for the handleable instances
  * @author Mikko Hilpinen
  * @since 10.5.2019, v1+
  */
trait EventHandler2[Listener <: Handleable2, -Event] extends Handler2[Listener]
{
	// ABSTRACT -----------------------
	
	/**
	  * Finds an event filter for the specified listener
	  * @param listener A listener
	  * @return An event filter used by the specified listener
	  */
	protected def filterOf(listener: Listener): Filter[Event]
	
	/**
	  * Informs a single listener about an event
	  * @param listener A listener
	  * @param event An event
	  */
	protected def deliver(listener: Listener, event: Event): Unit
	
	
	// OTHER    ----------------------
	
	/**
	  * Distributes an event between the listeners
	  * @param event An event
	  */
	def distribute(event: Event): Unit = items.foreach { l => if (filterOf(l)(event)) deliver(l, event) }
}

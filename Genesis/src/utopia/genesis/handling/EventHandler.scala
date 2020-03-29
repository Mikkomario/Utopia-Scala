package utopia.genesis.handling

import utopia.inception.handling.{Handleable, Handler}
import utopia.inception.util.Filter

/**
  * Event handlers distribute events for the handleable instances
  * @author Mikko Hilpinen
  * @since 10.5.2019, v1+
  */
trait EventHandler[Listener <: Handleable, Event] extends Handler[Listener]
{
	/**
	  * Finds an event filter for the specified listener
	  * @param listener A listener
	  * @return An event filter the listener uses
	  */
	protected def eventFilterFor(listener: Listener): Filter[Event]
	
	/**
	  * Informs a single listener about an event
	  * @param listener A listener
	  * @param event An event
	  * @return Arbitary return value
	  */
	protected def inform(listener: Listener, event: Event): Any
	
	/**
	  * Distributes an event between the listeners
	  * @param event An event
	  */
	def distribute(event: Event): Any = handle { l => if (eventFilterFor(l)(event)) inform(l, event) }
}

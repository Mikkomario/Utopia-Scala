package utopia.flow.view.template.eventful

import utopia.flow.event.listener.LazyResetListener

/**
  * Common trait for views which generate events whenever they are reset
  * @author Mikko Hilpinen
  * @since 25.7.2023, v2.2
  */
trait ResetListenable[+A]
{
	/**
	  * Adds a new listener so that it will be informed of any new reset events
	  * @param listener A listener to assign
	  */
	def addResetListener(listener: LazyResetListener[A]): Unit
	
	/**
	  * Adds a new reset listener to be informed of possible new reset events.
	  * If this container is currently empty (cleared / reset),
	  * simulates, for this listener, a reset event for the current state.
	  * @param simulatedOldValue A value used as the "old value" placeholder when simulating an event
	  * @param listener A listener to assign
	  * @tparam B Type of the listener and the simulated value
	  */
	def addResetListenerAndSimulateEvent[B >: A](simulatedOldValue: => B)(listener: LazyResetListener[B]): Unit
	
	/**
	  * Removes a reset listener from being informed of further reset events
	  * @param listener A listener to remove
	  */
	def removeResetListener(listener: Any): Unit
}

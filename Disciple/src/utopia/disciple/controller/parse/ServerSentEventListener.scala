package utopia.disciple.controller.parse

import utopia.access.model.event.{ServerSentEvent, StreamingServerSentEvent}

object ServerSentEventListener
{
	// OTHER    ---------------------------
	
	/**
	  * @param f A function called when a server-sent event is fully read
	  * @return A new listener which calls the specified function, but only processes events once they're fully read
	  */
	def apply(f: ServerSentEvent => Unit) = streamingAndCompleted { _ => () }(f)
	/**
	  * @param f A function that is called when as soon as a new event is received.
	  *          Receives the events in their streaming / pointer-based format.
	  * @return A new listener which calls the specified function
	  */
	def streaming(f: StreamingServerSentEvent => Unit) = streamingAndCompleted(f) { _ => () }
	
	/**
	  * @param onEventStarted A function called whenever a server-sent event is first received.
	  *                       Receives the events in their streaming (pointer-based) format.
	  * @param onEventCompleted A function called whenever a server-sent event is completed.
	  *                         Receives the events in their completed, immutable format.
	  * @return A new server-sent event listener
	  */
	def streamingAndCompleted(onEventStarted: StreamingServerSentEvent => Unit)
	         (onEventCompleted: ServerSentEvent => Unit): ServerSentEventListener =
		new _ServerSentEventListener(onEventStarted, onEventCompleted)
	
	
	// NESTED   ---------------------------
	
	private class _ServerSentEventListener(onStart: StreamingServerSentEvent => Unit,
	                                       onComplete: ServerSentEvent => Unit)
		extends ServerSentEventListener
	{
		override def onEventStarted(event: StreamingServerSentEvent): Unit = onStart(event)
		override def onEventCompleted(event: ServerSentEvent): Unit = onComplete(event)
	}
}

/**
  * Common trait for listeners that are interested in receiving server-sent events
  * @author Mikko Hilpinen
  * @since 31.03.2025, v1.9
  */
trait ServerSentEventListener
{
	/**
	  * Called when a new SSE is received (though not necessarily fully processed yet)
	  * @param event An event that was just constructed. May receive additional data over time.
	  */
	def onEventStarted(event: StreamingServerSentEvent): Unit
	/**
	  * Called when an SSE is completed
	  * @param event The received event
	  */
	def onEventCompleted(event: ServerSentEvent): Unit
}

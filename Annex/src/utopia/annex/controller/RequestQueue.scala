package utopia.annex.controller

import utopia.annex.model.request.ApiRequest
import utopia.flow.async.ActionQueue

import scala.concurrent.ExecutionContext

object RequestQueue
{
	// OTHER	--------------------------
	
	/**
	  * @param master Queue system this queue should use
	  * @param width How many requests can be handled at once (default = 1)
	  * @param exc Implicit execution context
	  * @return A new request queue
	  */
	def apply(master: QueueSystem, width: Int = 1)(implicit exc: ExecutionContext): RequestQueue =
		new SimpleRequestQueue(master, width)
	
	
	// NESTED	--------------------------
	
	private class SimpleRequestQueue(override val master: QueueSystem, width: Int = 1)(implicit exc: ExecutionContext)
		extends RequestQueue
	{
		override protected val queue = new ActionQueue(width)
	}
}

/**
  * A queue used for sending requests back to back
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait RequestQueue
{
	// ABSTRACT	-------------------------
	
	/**
	  * @return Action queue used for queueing request sends
	  */
	protected def queue: ActionQueue
	
	/**
	  * @return The system that handles the queued requests
	  */
	protected def master: QueueSystem
	
	
	// OTHER	-------------------------
	
	/**
	  * Pushes a new request to this queue
	  * @param request Request to push
	  * @return Asynchronous request result (either received response or reason why the request wasn't continued)
	  */
	def push(request: ApiRequest) =
		queue.push { master.pushSynchronous(request) }
}

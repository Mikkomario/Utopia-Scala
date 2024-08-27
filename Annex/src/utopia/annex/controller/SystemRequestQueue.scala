package utopia.annex.controller

import utopia.annex.model.request.RequestQueueable
import utopia.annex.model.response.RequestResult
import utopia.flow.async.context.ActionQueue
import utopia.flow.async.context.ActionQueue.QueuedAction
import utopia.flow.util.logging.Logger

import scala.concurrent.ExecutionContext

object SystemRequestQueue
{
	// OTHER	--------------------------
	
	/**
	  * @param master Queue system this queue should use
	  * @param width How many requests can be handled at once (default = 1)
	  * @param exc Implicit execution context
	  * @return A new request queue
	  */
	def apply(master: QueueSystem, width: Int = 1)(implicit exc: ExecutionContext, log: Logger): SystemRequestQueue =
		new SimpleRequestQueue(master, width)
	
	
	// NESTED	--------------------------
	
	private class SimpleRequestQueue(override val master: QueueSystem, width: Int = 1)
	                                (implicit exc: ExecutionContext, log: Logger)
		extends SystemRequestQueue
	{
		override protected val queue = new ActionQueue(width)
	}
}

/**
  * A queue used for sending requests back to back.
  * Delegates the requests to a [[QueueSystem]]
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait SystemRequestQueue extends RequestQueue
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
	
	
	// IMPLEMENTED	-------------------------
	
	/**
	  * Pushes a new request to this queue
	  * @param request Request to push.
	  *                Either
	  *                     Left: A request seed that will be converted into a request before sending it, or
	  *                     Right: A prepared request
	  * @return Asynchronous final request result
	  */
	def push[A](request: RequestQueueable[A]): QueuedAction[RequestResult[A]] =
		queue.push { master.pushBlocking(request) }
}

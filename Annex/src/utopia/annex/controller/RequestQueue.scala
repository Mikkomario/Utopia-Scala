package utopia.annex.controller

import utopia.annex.model.request.{ApiRequest, ApiRequestSeed}
import utopia.annex.model.response.RequestResult
import utopia.flow.async.context.ActionQueue

import scala.concurrent.{ExecutionContext, Future}

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
	  * @return Asynchronous final request result
	  */
	def push(request: ApiRequest): Future[RequestResult] = push(Right(request))
	/**
	  * Pushes a new request to this queue
	  * @param requestSeed A request seed that will be converted into a request before sending it
	  * @return Asynchronous final request result
	  */
	def push(requestSeed: ApiRequestSeed): Future[RequestResult] = push(Left(requestSeed))
	/**
	  * Pushes a new request to this queue
	  * @param request Request to push.
	  *                Either
	  *                     Left: A request seed that will be converted into a request before sending it, or
	  *                     Right: A prepared request
	  * @return Asynchronous final request result
	  */
	def push(request: Either[ApiRequestSeed, ApiRequest]) =
		queue.push { master.pushBlocking(request) }
}

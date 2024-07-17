package utopia.annex.controller

import utopia.annex.model.request.{ApiRequest, ApiRequestSeed, RequestQueueable}
import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.RequestResult
import utopia.flow.async.context.ActionQueue
import utopia.flow.async.context.ActionQueue.QueuedAction

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

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
		extends SystemRequestQueue
	{
		override protected val queue = new ActionQueue(width)
	}
}

/**
  * A queue used for sending requests back to back.
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait RequestQueue
{
	// ABSTRACT	-------------------------
	
	/**
	  * Pushes a new request to this queue
	  * @param request Request to push.
	  *                Either
	  *                     Left: A request seed that will be converted into a request before sending it, or
	  *                     Right: A prepared request
	  * @return Asynchronous final request result
	  */
	def push[A](request: RequestQueueable[A]): QueuedAction[RequestResult[A]]
	
	
	// OTHER    ------------------------
	
	/**
	  * Pushes a new request to this queue
	  * @param request Request to push
	  * @return Asynchronous final request result
	  */
	def push[A](request: ApiRequest[A]): QueuedAction[RequestResult[A]] = push(Right(request))
	/**
	  * Pushes a new request to this queue
	  * @param requestSeed A request seed that will be converted into a request before sending it
	  * @return Asynchronous final request result
	  */
	def push[A](requestSeed: ApiRequestSeed[A]): QueuedAction[RequestResult[A]] = push(Left(requestSeed))
	
	/**
	  * Pushes a new request to this queue, if one was successfully acquired
	  * @param request Request to push to this queue.
	  *                Failure if request-acquisition failed.
	  * @tparam A Type of the parsed response contents
	  * @return An action which resolves to the eventual request send result.
	  *         If 'request' was a failure, returns an immediate failure instead.
	  */
	def tryPush[A](request: Try[ApiRequest[A]]) = _tryPush(request.map { Right(_) })
	/**
	  * Pushes a new request to this queue in seed form, provided that one was successfully acquired
	  * @param requestSeed Request seed to push to this queue.
	  *                    This seed will be germinated into a request, if it is a Success.
	  *                    Failure if request-acquisition / seed-generation failed.
	  * @tparam A Type of the parsed response contents
	  * @return An action which resolves to the eventual request send result.
	  *         If 'request' was a failure, returns an immediate failure instead.
	  */
	def tryPushSeed[A](requestSeed: Try[ApiRequestSeed[A]]) =
		_tryPush(requestSeed.map { Left(_) })
	
	private def _tryPush[A](request: Try[RequestQueueable[A]]) = request match {
		case Success(request) => push(request)
		case Failure(error) => QueuedAction.completed(RequestSendingFailed(error))
	}
}

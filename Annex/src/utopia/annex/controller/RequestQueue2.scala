package utopia.annex.controller

import utopia.annex.model.request.{ApiRequest2, ApiRequestSeed2, RequestQueueable2}
import utopia.annex.model.response.RequestNotSent2.RequestSendingFailed2
import utopia.annex.model.response.RequestResult2
import utopia.flow.async.context.ActionQueue.QueuedAction

import scala.util.{Failure, Success, Try}

// TODO: Add a concrete function-wrapper implementation

/**
  * A queue used for sending requests back to back.
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  */
trait RequestQueue2
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
	def push[A](request: RequestQueueable2[A]): QueuedAction[RequestResult2[A]]
	
	
	// OTHER    ------------------------
	
	/**
	  * Pushes a new request to this queue
	  * @param request Request to push
	  * @return Asynchronous final request result
	  */
	def push[A](request: ApiRequest2[A]): QueuedAction[RequestResult2[A]] = push(Right(request))
	/**
	  * Pushes a new request to this queue
	  * @param requestSeed A request seed that will be converted into a request before sending it
	  * @return Asynchronous final request result
	  */
	def push[A](requestSeed: ApiRequestSeed2[A]): QueuedAction[RequestResult2[A]] = push(Left(requestSeed))
	
	/**
	  * Pushes a new request to this queue, if one was successfully acquired
	  * @param request Request to push to this queue.
	  *                Failure if request-acquisition failed.
	  * @tparam A Type of the parsed response contents
	  * @return An action which resolves to the eventual request send result.
	  *         If 'request' was a failure, returns an immediate failure instead.
	  */
	def tryPush[A](request: Try[ApiRequest2[A]]) = _tryPush(request.map { Right(_) })
	/**
	  * Pushes a new request to this queue in seed form, provided that one was successfully acquired
	  * @param requestSeed Request seed to push to this queue.
	  *                    This seed will be germinated into a request, if it is a Success.
	  *                    Failure if request-acquisition / seed-generation failed.
	  * @tparam A Type of the parsed response contents
	  * @return An action which resolves to the eventual request send result.
	  *         If 'request' was a failure, returns an immediate failure instead.
	  */
	def tryPushSeed[A](requestSeed: Try[ApiRequestSeed2[A]]) =
		_tryPush(requestSeed.map { Left(_) })
	
	private def _tryPush[A](request: Try[RequestQueueable2[A]]) = request match {
		case Success(request) => push(request)
		case Failure(error) => QueuedAction.completed(RequestSendingFailed2(error))
	}
}

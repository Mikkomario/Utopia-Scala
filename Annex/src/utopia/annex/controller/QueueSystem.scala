package utopia.annex.controller

import utopia.access.http.Status.ServiceUnavailable
import utopia.annex.model.request.{ApiRequest, ApiRequestSeed, Retractable}
import utopia.annex.model.response.RequestNotSent.{RequestSendingFailed, RequestWasDeprecated}
import utopia.annex.model.response.{RequestFailure, RequestResult, Response}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ActionQueue
import utopia.flow.async.process.Wait
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.operator.Identity
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.VolatileFlag
import utopia.flow.view.template.eventful.AbstractChanging

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * Used for queueing incoming requests and handling situations where connection is (temporarily) lost
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  * @param api Api-interface used for making the requests
  * @param offlineModeWaitThreshold A request timeout after which connection is considered offline (default = 30 seconds)
  * @param minOfflineDelay Smallest request send delay used in offline mode (default = 5 seconds)
  * @param maxOfflineDelay Largest request send delay used in offline mode (default = 2 minutes)
  * @param increaseOfflineDelay A function used for modifying the offline delay between consecutive send attempts.
  *                             The result will always be limited to the 'maxOfflineDelay'.
  *                             Default = double the wait period between attempts.
  */
class QueueSystem(api: Api, offlineModeWaitThreshold: FiniteDuration = 30.seconds,
				  minOfflineDelay: FiniteDuration = 5.seconds,
				  maxOfflineDelay: FiniteDuration = 2.minutes,
				  increaseOfflineDelay: FiniteDuration => FiniteDuration = _ * 2)
				 (implicit exc: ExecutionContext)
{
	// ATTRIBUTES   ------------------------------
	
	private val _onlineFlag = new VolatileFlag(true)
	private val offlineQueue = new ActionQueue()
	
	// Contains an infinite iterator that returns (offline) request delays
	private val requestDelayIteratorPointer = _onlineFlag.strongMap { online =>
		if (online)
			Iterator.continually(Duration.Zero)
		else
			Iterator.iterate(minOfflineDelay)(increaseOfflineDelay).takeWhile { _ < maxOfflineDelay } ++
				Iterator.continually(maxOfflineDelay)
	}
	
	
	// COMPUTED ---------------------------------
	
	/**
	  * @return A pointer that contains true while there is (presumably) a valid connection to the server.
	  *         Contains false during offline mode.
	  */
	def onlineFlag = _onlineFlag.valueView
	
	/**
	  * @return Whether the system is currently in online mode and functioning normally
	  */
	def isOnline = _onlineFlag.value
	/**
	  * @return Whether the system is currently in offline mode and functioning in a limited frequency and width
	  */
	def isOffline = !isOnline
	
	/**
	  * @return A read-only pointer that shows whether this system is currently online (true) or offline (false)
	  */
	def isOnlinePointer: AbstractChanging[Boolean] = _onlineFlag
	
	
	// OTHER    ----------------------------------
	
	
	/**
	  * Pushes a new request to this queue system.
	  * The request may be executed immediately, or delayed in case this system is currently in offline mode.
	  * The request is attempted until a non-503-response is received,
	  * the request is deprecated or an InterruptedException occurs.
	  * @param request Request to send to the api.
	  * @return Asynchronous final send result
	  */
	def push(request: ApiRequest): Future[RequestResult] = {
		// Case: Online => Processes requests in parallel
		if (isOnline)
			Future { sendBlocking(request, offlineMode = false, skipWait = true) }
		// Case: Offline => Queues the request-processing
		else
			offlineQueue.push { sendBlocking(request, offlineMode = true, skipWait = false) }
	}
	/**
	  * Pushes a new request to this queue system.
	  * The request may be executed immediately, or delayed in case this system is currently in offline mode.
	  * The request is attempted until a non-503-response is received,
	  * the request is deprecated or an InterruptedException occurs.
	  * @param requestSeed A request seed that will be converted into a request and sent to the API.
	  * @return Asynchronous final send result
	  */
	def push(requestSeed: ApiRequestSeed): Future[RequestResult] = push(Left(requestSeed))
	/**
	  * Pushes a new request to this queue system.
	  * The request may be executed immediately, or delayed in case this system is currently in offline mode.
	  * The request is attempted until a non-503-response is received,
	  * the request is deprecated or an InterruptedException occurs.
	  * @param request Request to send to the api.
	  *                Either Right: A prepared request to send, or
	  *                Left: A request seed that will be converted into a request upon sending.
	  * @return Asynchronous final send result
	  */
	def push(request: Either[ApiRequestSeed, ApiRequest]): Future[RequestResult] = {
		// In online mode, performs the requests side by side (i.e. parallel)
		if (isOnline)
			Future { sendBlocking(request).rightOrMap(handleDelayedRequestBlocking) }
		// In offline mode, pushes requests to the queue
		else
			offlineQueue.push { sendBlocking(request, offlineMode = true) }.flatMap {
				// Case: Request-resolution was delayed => Waits for the request to resolve
				case Left(delayedRequestFuture) =>
					delayedRequestFuture.flatMap {
						// Case: Request successfully resolved => Adds it to the queue with high priority
						case Success(request) =>
							offlineQueue.prepend { sendBlocking(request, offlineMode = true, skipWait = false) }
						// Case: Request-resolution failed => Returns a failure
						case Failure(error) => Future.successful(RequestSendingFailed(error))
					}
				// Case: Request completed => Returns the result
				case Right(result) => Future.successful(result)
			}
	}
	
	/**
	  * Pushes a new request to this queue system.
	  * The request may be executed immediately, or delayed in case this system is currently in offline mode.
	  * The request is attempted until a non-503-response is received,
	  * the request is deprecated or an InterruptedException occurs.
	  *
	  * This method call blocks. In case of internet connection problems or API-unavailability (503),
	  * the blocking period may be quite extensive.
	  *
	  * @param request Request to send to the api.
	  *
	  * @return Final send result, once acquired
	  */
	def pushBlocking(request: ApiRequest): RequestResult = {
		// Case: Online => Processes the requests in parallel
		if (isOnline)
			sendBlocking(request, offlineMode = false, skipWait = true)
		// Case: Offline => Queues the request
		else
			offlineQueue.push { sendBlocking(request, offlineMode = true, skipWait = false) }
				.waitFor().getOrMap(RequestSendingFailed.apply)
	}
	/**
	  * Pushes a new request to this queue system.
	  * The request may be executed immediately, or delayed in case this system is currently in offline mode.
	  * The request is attempted until a non-503-response is received,
	  * the request is deprecated or an InterruptedException occurs.
	  *
	  * This method call blocks. In case of internet connection problems or API-unavailability (503),
	  * the blocking period may be quite extensive.
	  *
	  * @param requestSeed A request seed that will be converted into a request and sent to the API.
	  *
	  * @return Final send result, once acquired
	  */
	def pushBlocking(requestSeed: ApiRequestSeed): RequestResult = pushBlocking(Left(requestSeed))
	/**
	  * Pushes a new request to this queue system.
	  * The request may be executed immediately, or delayed in case this system is currently in offline mode.
	  * The request is attempted until a non-503-response is received,
	  * the request is deprecated or an InterruptedException occurs.
	  *
	  * This method call blocks. In case of internet connection problems or API-unavailability (503),
	  * the blocking period may be quite extensive.
	  *
	  * @param request Request to send to the api.
	  *                Either Right: A prepared request to send, or
	  *                Left: A request seed that will be converted into a request upon sending.
	  *
	  * @return Final send result, once acquired
	  */
	def pushBlocking(request: Either[ApiRequestSeed, ApiRequest]): RequestResult = {
		// In online mode, performs the requests side by side
		if (isOnline)
			sendBlocking(request).rightOrMap(handleDelayedRequestBlocking)
		// In offline mode, pushes requests to the queue
		else
			offlineQueue.push { sendBlocking(request, offlineMode = true) }.waitFor() match {
				case Success(result) => result.rightOrMap(handleDelayedRequestBlocking)
				case Failure(error) => RequestSendingFailed(error)
			}
	}
	@deprecated("Renamed to pushBlocking(ApiRequest)", "v1.7")
	def pushSynchronous(request: ApiRequest): RequestResult = pushBlocking(request)
	
	private def handleDelayedRequestBlocking(requestFuture: Future[Try[ApiRequest]]) = {
		requestFuture.waitForResult()
			.flatMap { request =>
				// Case: Request-conversion succeeded => Queues the request with high priority
				offlineQueue.prepend(sendBlocking(request, offlineMode = true, skipWait = false)).waitFor()
			}
			.getOrMap(RequestSendingFailed.apply)
	}
	
	private def goOffline() = _onlineFlag.reset()
	private def goOnline() = _onlineFlag.set()
	
	// Returns either:
	//      Left: Future that may resolve into an API-request to send (delayed seed germination in sequential -use-case)
	//      Right: Request result (blocks) (parallel & offline request -use-cases)
	private def sendBlocking(request: Either[ApiRequestSeed, ApiRequest],
	                         offlineMode: Boolean = false): Either[Future[Try[ApiRequest]], RequestResult] =
	{
		afterPossibleWait[Either[Future[Try[ApiRequest]], RequestResult]](request.either,
			waitEnabled = offlineMode) { Right(_) } {
			request match {
				case Left(seed) =>
					// Converts the request seed into an actual request before sending it
					// If request processing is done sequentially,
					// the conversion is completed asynchronously and request processing may be delayed
					val requestFuture = seed.toRequest
					// Case: Sequential processing mode (offline or resolving offline queue)
					// => Mustn't block during request conversion because that could cause deadlocks.
					// Instead, converts the request asynchronously and pushes it to the queue once it's ready
					val immediateResult = {
						if (offlineMode)
							requestFuture.currentResult match {
								// Case: Request conversion completed immediately => Continues processing
								case Some(resolvedRequest) => Right(resolvedRequest.flatten)
								// Case: Request conversion will still take some time => Continues asynchronously
								case None => Left(requestFuture)
							}
						// Case: Parallel processing mode (online only) => Allowed to block during request-conversion
						else
							Right(requestFuture.waitForResult())
					}
					immediateResult.mapRight {
						case Success(request) => sendBlocking(request, offlineMode, skipWait = true)
						case Failure(error) => RequestSendingFailed(error)
					}
				case Right(request) => Right(sendBlocking(request, offlineMode, skipWait = true))
			}
		}
	}
	private def sendBlocking(request: ApiRequest, offlineMode: Boolean, skipWait: Boolean): RequestResult = {
		afterPossibleWait[RequestResult](request, waitEnabled = offlineMode && !skipWait)(Identity) {
			val resultFuture = api.sendRequest(request)
			// Waits for a limited time first, in order to activate offline mode early enough
			// (request is not cancelled, however, and wait continues)
			val result = resultFuture.waitFor(offlineModeWaitThreshold) match {
				case Success(result) => Right(result)
				// Case: Offline timeout reached => Goes into offline mode and continues the wait
				case Failure(_) =>
					goOffline()
					// Continues the wait
					resultFuture.waitFor() match {
						// Case: Some result acquired
						case Success(result) => Right(result)
						// Case: No result acquired
						case Failure(error) => Left(RequestSendingFailed(error))
					}
			}
			result match {
				// Case: No result acquired (wait interrupted) => Fails
				case Left(failure) => failure
				// Case: Result acquired => Processes it
				case Right(result) =>
					result match {
						// Case: Response acquired => Returns (unless 503)
						case response: Response if response.status != ServiceUnavailable =>
							goOnline()
							response
						case _ =>
							// On connection failure (or 503), tries again
							goOffline()
							// Case: Was already in offline mode => Reattempts the request after a while
							if (offlineMode)
								sendBlocking(request, offlineMode = true, skipWait = false)
							// Case: Just went offline => Queues the request so that only one attempt is made at a time
							else
								offlineQueue.push { sendBlocking(request, offlineMode = true, skipWait = false) }
									.waitFor() match
								{
									case Success(result) => result
									case Failure(error) => RequestSendingFailed(error)
								}
					}
			}
		}
	}
	private def afterPossibleWait[A](request: Retractable, waitEnabled: Boolean)(fail: RequestFailure => A)(f: => A) = {
		// Delays the request when in offline mode
		val waitInterrupted = {
			if (waitEnabled && isOffline)
				!Wait(requestDelayIteratorPointer.value.next())
			else
				false
		}
		// May skip request sending altogether if the request gets deprecated
		if (request.deprecated)
			fail(RequestWasDeprecated)
		else if (waitInterrupted)
			fail(RequestSendingFailed(new InterruptedException("Request-sending was interrupted during offline wait")))
		else
			f
	}
}

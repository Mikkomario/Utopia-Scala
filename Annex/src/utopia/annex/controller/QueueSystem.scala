package utopia.annex.controller

import java.time.Instant
import utopia.annex.model.request.ApiRequest
import utopia.annex.model.response.RequestNotSent.{RequestFailed, RequestWasDeprecated}
import utopia.annex.model.response.{RequestNotSent, Response}
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ActionQueue
import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.Now
import utopia.flow.view.mutable.async.VolatileFlag
import utopia.flow.view.template.eventful.AbstractChanging

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

/**
  * Used for queueing incoming requests and handling situations where connection is (temporarily) lost
  * @author Mikko Hilpinen
  * @since 17.6.2020, v1
  * @param api Api-interface used for making the requests
  * @param offlineModeWaitThreshold A request timeout after which connection is considered offline (default = 30 seconds)
  * @param minOfflineDelay Smallest request send delay used in offline mode (default = 5 seconds)
  * @param maxOfflineDelay Largest request send delay used in offline mode (default = 2 minutes)
  * @param offlineDelayIncreaseModifier A modifier applied to the request send delay on each failed request
  *                                     (default = 2.0 = wait time doubles)
  */
class QueueSystem(api: Api, offlineModeWaitThreshold: FiniteDuration = 30.seconds,
				  minOfflineDelay: FiniteDuration = 5.seconds,
				  maxOfflineDelay: FiniteDuration = 2.minutes, offlineDelayIncreaseModifier: Double = 2.0)
				 (implicit exc: ExecutionContext)
{
	// ATTRIBUTES   ------------------------------
	
	private val isOnlineFlag = new VolatileFlag(true)
	private val offlineQueue = new ActionQueue()
	
	private var nextOfflineDelay = minOfflineDelay
	private var nextOfflineMinRequestTime = Instant.now()
	
	
	// COMPUTED ---------------------------------
	
	/**
	  * @return Whether the system is currently in online mode and functioning normally
	  */
	def isOnline = isOnlineFlag.value
	
	/**
	  * @return Whether the system is currently in offline mode and functioning in a limited frequency and width
	  */
	def isOffline = !isOnline
	
	/**
	  * @return A read-only pointer that shows whether this system is currently online (true) or offline (false)
	  */
	def isOnlinePointer: AbstractChanging[Boolean] = isOnlineFlag
	
	
	// OTHER    ----------------------------------
	
	/**
	  * Pushes a new request to this queue system. The request may be executed immediately, or delayed in case this system
	  * is currently in offline mode. The request is attempted until a response is received
	  * @param request Request to send to the api
	  * @return Asynchronous response (Either Right: Response or Left: Request send failure)
	  */
	def push(request: ApiRequest): Future[Either[RequestNotSent, Response]] =
	{
		// In online mode, performs the requests side by side
		if (isOnline)
			Future { sendSynchronous(request) }
		// In offline mode, pushes requests to the queue
		else
			offlineQueue.push { sendSynchronous(request, isOfflineMode = true) }
	}
	
	/**
	  * Pushes a new request to this queue system. The request may be executed immediately, or delayed in case this system
	  * is currently in offline mode. The request is attempted until a response is received
	  * @param request Request to send to the api
	  * @return Asynchronous response (Either Right: Response or Left: Request send failure)
	  */
	def pushSynchronous(request: ApiRequest): Either[RequestNotSent, Response] =
	{
		// In online mode, performs the requests side by side
		if (isOnline)
			sendSynchronous(request)
		// In offline mode, pushes requests to the queue
		else
		{
			offlineQueue.push { sendSynchronous(request, isOfflineMode = true) }.waitFor() match
			{
				case Success(result) => result
				case Failure(error) => Left(RequestFailed(error))
			}
		}
	}
	
	private def goOffline() = {
		isOnlineFlag.reset()
		nextOfflineMinRequestTime = Now + nextOfflineDelay
	}
	private def goOnline() = {
		if (isOnlineFlag.set()) {
			nextOfflineDelay = minOfflineDelay
			nextOfflineMinRequestTime = Now
		}
	}
	
	private def sendSynchronous(request: ApiRequest, isOfflineMode: Boolean = false): Either[RequestNotSent, Response] =
	{
		// Delays the request when in offline mode
		if (isOfflineMode && isOffline)
		{
			// Doubles the delay between each consequent failed request
			nextOfflineDelay = (nextOfflineDelay * offlineDelayIncreaseModifier).finite match
			{
				case Some(finite) => finite min maxOfflineDelay
				case None => maxOfflineDelay
			}
			val targetTime = nextOfflineMinRequestTime
			nextOfflineMinRequestTime = Now + nextOfflineDelay
			Wait(targetTime)
		}
		
		// May skip request sending altogether if the request gets deprecated
		if (request.isDeprecated)
			Left(RequestWasDeprecated)
		else
		{
			val resultFuture = api.sendRequest(request)
			
			// Waits for a limited time first, in order to activate offline mode early enough
			// (request is not cancelled, however, and wait continues)
			val result = resultFuture.waitFor(offlineModeWaitThreshold) match
			{
				case Success(result) => Right(result)
				case Failure(_) =>
					goOffline()
					resultFuture.waitFor() match
					{
						case Success(result) => Right(result)
						case Failure(error) => Left(RequestFailed(error))
					}
			}
			
			result match
			{
				case Left(failure) => Left(failure)
				case Right(result) =>
					result match
					{
						case Success(response) =>
							goOnline()
							Right(response)
						case Failure(_) =>
							// On connection failure, tries again
							goOffline()
							if (isOfflineMode)
								sendSynchronous(request, isOfflineMode = true)
							else
								offlineQueue.push { sendSynchronous(request, isOfflineMode = true) }.waitFor() match
								{
									case Success(result) => result
									case Failure(error) => Left(RequestFailed(error))
								}
					}
			}
		}
	}
}

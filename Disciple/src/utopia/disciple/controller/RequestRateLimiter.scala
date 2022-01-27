package utopia.disciple.controller

import utopia.flow.async.{Breakable, Volatile}
import utopia.flow.collection.VolatileList
import utopia.flow.time.{Now, WaitUtils}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.CollectionExtensions._

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

object RequestRateLimiter
{
	/**
	  * Creates a new request rate limiter instance
	  * @param maxRequestAmount Maximum number of request within the specified time period
	  * @param resetDuration Length of each considered time period
	  * @tparam A Type of future results
	  * @return A new request rate limiter instance
	  */
	def apply[A](maxRequestAmount: Int, resetDuration: FiniteDuration) =
		new RequestRateLimiter(maxRequestAmount, resetDuration)
	/**
	  * Creates a new request rate limiter instance
	  * @param maxRequestsPerSecond Maximum number of request that can be performed during a single second
	  * @tparam A Type of future results
	  * @return A new request rate limiter instance
	  */
	def maxPerSecond[A](maxRequestsPerSecond: Int) = new RequestRateLimiter(maxRequestsPerSecond, 1.seconds)
}

/**
  * Used for limiting the number of consecutive or simultaneous requests so that a possible rate limit is not exceeded
  * @author Mikko Hilpinen
  * @since 17.11.2021, v1.4.4
  */
class RequestRateLimiter(maxRequestAmount: Int, resetDuration: FiniteDuration) extends Breakable
{
	// ATTRIBUTES   --------------------------------
	
	private val requestTimes = VolatileList[Instant]()
	
	private lazy val waitLock = new AnyRef()
	// Each request accepts whether it should be completed (true) or immediately failed (false)
	private lazy val pendingRequests = VolatileList[Boolean => Future[_]]()
	private lazy val pendingClearedFuture = Volatile[Future[Unit]](Future.successful(()))
	
	
	// COMPUTED ------------------------------------
	
	private def recentRequestTimes = {
		val threshold = Now - resetDuration
		requestTimes.updateAndGet { v => v.takeRightWhile { _ > threshold } }
	}
	private def currentRequestCount = recentRequestTimes.size
	
	// Returns smallest wait time. None if no wait is necessary.
	private def nextAvailableRequestTime = {
		val times = requestTimes.value
		// Case: No wait needed
		if (times.size < maxRequestAmount)
			None
		// Case: Current requests full. Takes the first recent request and checks
		// when enough time has passed since it was performed
		else
			Some(times(times.size - maxRequestAmount) + resetDuration).filter { _.isInFuture }
	}
	
	
	// IMPLEMENTED  --------------------------------
	
	override def stop() = {
		// Fails all pending requests and hurries the clearance process to its completion
		pendingRequests.popAll().foreach { _(false) }
		WaitUtils.notify(waitLock)
		pendingClearedFuture.value
	}
	
	
	// OTHER    ------------------------------------
	
	/**
	  * Adds a new request to be performed
	  * @param makeRequest A function that starts the asynchronous request completion process
	  * @param exc Implicit execution context (used if the request has to be delayed)
	  * @tparam A Type of future contents / result
	  * @return Request completion future
	  */
	def push[A](makeRequest: => Future[A])(implicit exc: ExecutionContext) = {
		// Checks whether the request can be performed immediately or whether it should be added to pending requests
		// Case: Request can be performed immediately => does so and records when the request was performed
		if (currentRequestCount < maxRequestAmount) {
			requestTimes :+= Now
			makeRequest
		}
		// Case: Request can't be performed right now => delays it
		else {
			// Tracks request completion with a promise
			val promise = Promise[A]()
			// Queues the request and eventually completes the promise with it
			pendingRequests :+= { isAllowedToRun =>
				if (isAllowedToRun) {
					// Catches errors thrown during request creation
					val request = Try { makeRequest } match {
						case Success(request) => request
						case Failure(error) => Future.failed(error)
					}
					promise.completeWith(request)
					request
				}
				else
					Future.failed(new InterruptedException(
						"RequestRateLimiter clearance process was interrupted with .stop()"))
			}
			// Starts the clearance process if it isn't already active
			pendingClearedFuture.setIf { _.isCompleted } {
				Future {
					// This clearance process continues as long as there are pending requests to clear
					while (pendingRequests.nonEmpty) {
						// Waits the smallest possible time until performing the next request
						nextAvailableRequestTime.foreach { WaitUtils.waitUntil(_, waitLock) }
						// Records request time and performs it asynchronously
						requestTimes :+= Now
						pendingRequests.pop().foreach { _(true) }
					}
				}
			}
			promise.future
		}
	}
}
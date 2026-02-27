package utopia.annex.controller

import utopia.access.model.enumeration.Method
import utopia.annex.model.request.{ApiRequest, ApiRequestSeed, QueuedRequest, RequestQueueable}
import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.RequestResult
import utopia.disciple.model.request.Body
import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ActionQueue
import utopia.flow.async.context.ActionQueue.QueuedAction
import utopia.flow.collection.immutable.Empty
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.operator.MaybeEmpty
import utopia.flow.time.Now
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object LockingRequestQueue
{
	// COMPUTED   ----------------------------
	
	/**
	 * @return A request queue that has already been locked
	 */
	def alreadyLocked: LockingRequestQueue = AlreadyLockedRequestQueue
	
	
	// OTHER    ------------------------------
	
	/**
	 * @param queue A queue to wrap
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation
	 * @return A LockingRequestQueue that never locks
	 */
	def neverLocking(queue: RequestQueue)(implicit exc: ExecutionContext, log: Logger): LockingRequestQueue =
		new NeverLockingRequestQueue(queue)
	
	/**
	 * Wraps another queue, adding locking implementation
	 * @param queue Queue to wrap
	 * @param lockedFlag A flag that contains true once the queue should be locked, making it unusable
	 * @param exc Implicit execution context
	 * @param log Implicit logging implementation
	 * @return A locking interface to the specified queue
	 */
	def wrap(queue: RequestQueue, lockedFlag: Flag)(implicit exc: ExecutionContext, log: Logger): LockingRequestQueue =
		lockedFlag.fixedValue match {
			// Case: Lock status is fixed => Uses a simpler implementation
			case Some(fixedState) =>
				if (fixedState)
					AlreadyLockedRequestQueue
				else
					new NeverLockingRequestQueue(queue)
			// Case: Lock status is not final => Uses the regular implementation
			case None => new _LockingRequestQueue(queue, lockedFlag)
		}
	
	
	// NESTED   ------------------------------
	
	private object AlreadyLockedRequestQueue extends LockingRequestQueue
	{
		// ATTRIBUTES   ----------------------
		
		override val lockedFlag: Flag = AlwaysTrue
		override val pendingRequests: Seq[QueuedRequest[_]] = Empty
		override val pendingRequestCountPointer: Changing[Int] = Fixed(0)
		
		
		// IMPLEMENTED  ----------------------
		
		override def stopFuture: Future[Unit] = Future.unit
		
		override def push[A](request: RequestQueueable[A]): QueuedAction[RequestResult[A]] =
			QueuedAction.completed(RequestSendingFailed(new IllegalStateException("This queue is locked")))
	}
	
	private class NeverLockingRequestQueue(wrapped: RequestQueue)(implicit exc: ExecutionContext, log: Logger)
		extends LockingRequestQueue
	{
		// ATTRIBUTES   ----------------------
		
		override val lockedFlag: Flag = AlwaysFalse
		
		private val pendingRequestsP = Volatile.eventful.emptySeq[QueuedRequest[_]]
		override val pendingRequestCountPointer: Changing[Int] = pendingRequestsP.lightMap { _.size }
		
		
		// IMPLEMENTED  ----------------------
		
		override def pendingRequests: Seq[QueuedRequest[_]] = pendingRequestsP.value
		
		override def stopFuture: Future[Unit] = Future.never
		
		override def push[A](request: RequestQueueable[A]): QueuedAction[RequestResult[A]] = {
			// Queues the request
			val queueTime = Now.toInstant
			val result = wrapped.push(request)
			// Updates the pending requests -pointer now and once the request resolves
			pendingRequestsP :+= QueuedRequest(request, result, queueTime)
			result.future.onComplete { _ => pendingRequestsP.update { _.filterNot { _.request == request } } }
			
			result
		}
	}
	
	private class _LockingRequestQueue(wrapped: RequestQueue, override val lockedFlag: Flag)
	                                  (implicit exc: ExecutionContext, log: Logger)
		extends LockingRequestQueue
	{
		// ATTRIBUTES   ------------------------
		
		private val pendingRequestsP = Volatile.lockable.emptySeq[QueuedRequest[_]]
		override val pendingRequestCountPointer = pendingRequestsP.lightMap { _.size }
		
		// Just in case someone resets the lock, we'll respond by resetting this future
		private val lazyStopFuture = Lazy.resettable {
			queueStopFutureReset()
			lockedFlag.future.flatMap { _ => pendingRequestsP.emptyFuture }
		}
		private val resetStopFutureListener = ChangeListener[Boolean] { e =>
			if (e.newValue)
				Continue
			else {
				lazyStopFuture.reset()
				Detach
			}
		}
		
		
		// IMPLEMENTED  ------------------------
		
		override def pendingRequests: Seq[QueuedRequest[_]] = pendingRequestsP.value
		
		override def stopFuture = lazyStopFuture.value
		
		override def push[A](request: RequestQueueable[A]): ActionQueue.QueuedAction[RequestResult[A]] = {
			// Case: Locked => Request fails automatically
			if (locked)
				QueuedAction.completed(RequestSendingFailed(new IllegalStateException("This queue has been locked")))
			else {
				// Queues the request and records it
				val result = pendingRequestsP.lockWhile {
					// Uses a special wrapper class in order to apply deprecation on locking
					val queueTime = Now.toInstant
					val result = wrapped.push(
						request.mapBoth { new RequestSeedWrapper[A](_) } { new RequestWrapper[A](_) })
					pendingRequestsP :+= QueuedRequest(request, result, queueTime)
					result
				}
				// Once the request resolves, updates the record of pending requests
				result.future.onComplete { _ =>
					pendingRequestsP.lockWhile {
						val isEmpty = pendingRequestsP.mutate { requests =>
							val remaining = requests.filterNot { _.request == request }
							remaining.isEmpty -> remaining
						}
						// Case: This queue was cleared after stop => Locks the pending requests -pointer
						if (isEmpty && locked)
							pendingRequestsP.lock()
					}
				}
				result
			}
		}
		
		
		// OTHER    ----------------------------
		
		private def queueStopFutureReset(): Unit = lockedFlag.addListener(resetStopFutureListener)
		
		
		// NESTED   ----------------------------
		
		private class RequestWrapper[+A](wrapped: ApiRequest[A]) extends ApiRequest[A]
		{
			override def method: Method = wrapped.method
			override def path: String = wrapped.path
			override def pathParams: Model = wrapped.pathParams
			
			override def body: Either[Value, Body] = wrapped.body
			
			override def deprecated: Boolean = locked || wrapped.deprecated
			
			override def send(prepared: ApiClient.PreparedRequest): Future[RequestResult[A]] = wrapped.send(prepared)
		}
		private class RequestSeedWrapper[+A](wrapped: ApiRequestSeed[A]) extends ApiRequestSeed[A]
		{
			override def persistingModelPointer: Changing[Option[Model]] = wrapped.persistingModelPointer
			override def deprecated: Boolean = locked || wrapped.deprecated
			
			override def toRequest: Future[Try[ApiRequest[A]]] = wrapped.toRequest.mapSuccess { new RequestWrapper[A](_) }
		}
	}
}

/**
 * Common trait for request queues that may become locked, preventing further requests
 * @author Mikko Hilpinen
 * @since 26.02.2026, v1.12
 */
trait LockingRequestQueue extends RequestQueue with MaybeEmpty[LockingRequestQueue]
{
	// ABSTRACT ----------------------------
	
	/**
	 * @return A flag that will contain true once this queue becomes locked and no longer accepts new requests.
	 */
	def lockedFlag: Flag
	
	/**
	 * @return Requests that are currently waiting to be executed, or being executed
	 */
	def pendingRequests: Seq[QueuedRequest[_]]
	/**
	 * A pointer that contains the number of requests that are currently queued,
	 * whether active (i.e. being executed) or waiting.
	 */
	def pendingRequestCountPointer: Changing[Int]
	
	/**
	 * A future that resolves once this queue has been locked AND then finished all pending requests.
	 */
	def stopFuture: Future[Unit]
	
	
	// COMPUTED ----------------------------
	
	/**
	 * @return Whether this queue has been locked
	 */
	def locked = lockedFlag.value
	/**
	 * @return Whether this queue still remains unlocked & usable
	 */
	def unlocked = !locked
	
	/**
	 * @return Whether this queue has been locked, and has completed all requests
	 */
	def stopped = locked && isEmpty
	
	/**
	 * @return The current number of requests being executed
	 */
	def pendingRequestCount = pendingRequestCountPointer.value
	
	
	// IMPLEMENTED  ------------------------
	
	override def self: LockingRequestQueue = this
	override def isEmpty: Boolean = pendingRequestCount == 0
}

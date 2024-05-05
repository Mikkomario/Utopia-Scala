package utopia.flow.async.context

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.ProcessState.{BasicProcessState, Completed, NotStarted, Running}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.view.mutable.async.{Volatile, VolatileOption}
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions
import scala.util.Try

object ActionQueue
{
	// NESTED   ------------------------
	
	object QueuedAction
	{
		implicit def autoAccessFuture[A](a: QueuedAction[A]): Future[A] = a.future
	}
	/**
	  * Common trait for actions that have been queued in an ActionQueue.
	  * This interface is the public front for these actions.
	  * @tparam A Type of result yielded by this action.
	  */
	trait QueuedAction[+A]
	{
		// ABSTRACT -----------------------
		
		/**
		  * @return Future of the completion of this action
		  */
		def future: Future[A]
		/**
		  * @return Future that resolves once this action starts to run
		  */
		def startFuture: Future[Unit]
		
		/**
		  * @return Pointer that contains the current state of this action.
		  *         I.e. whether this action has started and/or finished running.
		  */
		def statePointer: Changing[BasicProcessState]
		
		
		// COMPUTED -----------------------
		
		/**
		  * @return The current state of this action
		  */
		def state = statePointer.value
		
		
		// OTHER    -----------------------
		
		/**
		  * Blocks until this action has finished running
		  * @param timeout Maximum time to wait. Default = infinite.
		  * @return Action result. Failure if timeout was reached or if the process threw.
		  */
		def waitFor(timeout: Duration = Duration.Inf) = future.waitFor(timeout)
	}
	
	private abstract class InteractiveAction[+A] extends QueuedAction[A] with Runnable
	{
		// ATTRIBUTES   ------------------
		
		private val _statePointer = Volatile[BasicProcessState](NotStarted)
		
		override lazy val startFuture: Future[Unit] = _statePointer.findMapFuture {
			case Completed => Some(())
			case _ => None
		}
		
		
		// COMPUTED ----------------------
		
		protected def state_=(newState: BasicProcessState) = _statePointer.value = newState
		
		
		// IMPLEMENTED  ------------------
		
		override def statePointer: Changing[BasicProcessState] = _statePointer.readOnly
	}
	
	private class SureAction[A](action: => A) extends InteractiveAction[A]
	{
		// ATTRIBUTES	------------------
		
		private val promise = Promise[A]()
		
		
		// IMPLEMENTED	------------------
		
		override def future = promise.future
		
		override def run() = {
			state = Running
			promise.complete(Try(action))
			state = Completed
		}
	}
	private class UnsureAction[A](action: => Try[A]) extends InteractiveAction[A]
	{
		// ATTRIBUTES	-----------------
		
		private val promise = Promise[A]()
		
		
		// IMPLEMENTED	-----------------
		
		override def future = promise.future
		
		override def run() = {
			state = Running
			promise.complete(action)
			state = Completed
		}
	}
	private class AsyncAction[A](startFuture: => Future[A]) extends InteractiveAction[A]
	{
		// ATTRIBUTES   ----------------
		
		// Contains Some(Left) if requested before this action is started
		// Contains Some(Right) if requested after this action has started
		// Contains None before requested
		private val wrappedPointer = VolatileOption[Either[Promise[A], Future[A]]]()
		
		
		// IMPLEMENTED  ---------------
		
		// Prepares a future if one hasn't been prepared already
		override def future = wrappedPointer.mutate {
			// Case: Future already prepared => Returns that
			case Some(wrapped) => wrapped.rightOrMap { _.future } -> Some(wrapped)
			// Case: No future prepared => Forms a promise and returns the future of that promise
			case None =>
				val promise = Promise[A]()
				promise.future -> Some(Left(promise))
		}
		
		override def run() = {
			state = Running
			// Updates or sets up the wrapped promise/future
			val (promise, future) = wrappedPointer.mutate[(Option[Promise[A]], Future[A])] {
				case Some(wrapped) =>
					wrapped match {
						// Case: Promise already prepared => Prepares to complete that promise with this new future
						case Left(promise) => (Some(promise), startFuture) -> Some(Left(promise))
						// Case: Future already prepared (shouldn't be) => Returns that future
						case Right(future) => (None, future) -> Some(Right(future))
					}
				// Case: No promise prepared => Skips the promise creation and wraps the new future
				case None =>
					val future = startFuture
					(None, future) -> Some(Right(future))
			}
			// Blocks during the asynchronous process completion
			// When the wrapped future completes, completes the promise (if applicable) and updates the state
			val res = future.waitFor()
			promise.foreach { _.complete(res) }
			state = Completed
		}
	}
}

/**
  * ActionQueues are used for queueing actions back to back. Multiple actions can still be completed
  * at once
  * @author Mikko Hilpinen
  * @since 22.5.2019, v1.4.1+
  */
class ActionQueue(val maxWidth: Int = 1)(implicit context: ExecutionContext)
{
	import ActionQueue._
	
	// ATTRIBUTES	------------------
	
	private val queue = VolatileList[InteractiveAction[_]]()
	private val handleCompletions = VolatileList[Future[_]]()
	
	/**
	  * A pointer that contains the number of queued (waiting) items in this queue at any time.
	  */
	lazy val queueSizePointer = queue.readOnly.map { _.size }
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return An execution context that uses this action queue
	  */
	def asExecutionContext = QueuedExecutionContext
	
	
	// OTHER	----------------------
	
	/**
	  * Pushes a new operation to the end of this queue
	  * @param operation An operation
	  * @tparam A Operation result type
	  * @return The queued action
	  */
	def push[A](operation: => A): QueuedAction[A] = _push(new SureAction(operation))
	/**
	  * Pushes a new operation to the end of this queue
	  * @param operation An operation that returns a Try
	  * @tparam A Operation result type (inside try)
	  * @return The queued action
	  */
	def pushTry[A](operation: => Try[A]) = _push(new UnsureAction(operation))
	/**
	  * Pushes a new operation to the end of this queue
	  * @param resolvesAsync An operation that completes asynchronously, returning a Future.
	  * @tparam A Type of future the operation resolves into
	  * @return The queued action
	  */
	def pushAsync[A](resolvesAsync: => Future[A]) = _push(new AsyncAction[A](resolvesAsync))
	
	/**
	  * Pushes a new operation to this queue. Passes all other operations that have been queued.
	  * @param operation An operation
	  * @tparam A Operation result type
	  * @return The action that was queued
	  */
	def prepend[A](operation: => A): QueuedAction[A] = _push(new SureAction[A](operation), prepend = true)
	/**
	  * Pushes a new operation to the end of this queue. Passes all other operations that have been queued.
	  * @param resolvesAsync An operation that completes asynchronously, returning a Future.
	  * @tparam A Type of future the operation resolves into
	  * @return The action that was queued
	  */
	def prependAsync[A](resolvesAsync: => Future[A]): QueuedAction[A] =
		_push(new AsyncAction[A](resolvesAsync), prepend = true)
	
	private def _push[A](action: InteractiveAction[A], prepend: Boolean = false): QueuedAction[A] = {
		// Pushes or prepends the action to queue
		if (prepend) queue.update { action +: _ } else queue :+= action
		// Starts additional handlers if possible
		handleCompletions.update { current =>
			val incomplete = current.filterNot { _.isCompleted }
			if (incomplete.hasSize < maxWidth)
				incomplete :+ Future { handle() }
			else
				incomplete
		}
		
		action
	}
	
	private def next() = queue.pop()
	
	// Handles actions consecutively as long as there are some available
	private def handle() = OptionsIterator.continually { next() }.foreach { _.run() }
	
	
	// NESTED   -----------------------
	
	object QueuedExecutionContext extends ExecutionContext
	{
		override def execute(runnable: Runnable) = push { runnable.run() }
		override def reportFailure(cause: Throwable) = context.reportFailure(cause)
	}
}

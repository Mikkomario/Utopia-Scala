package utopia.flow.async.context

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ActionQueue.{AsyncAction, InteractiveAction, QueuedAction, SyncAction}
import utopia.flow.async.process.ProcessState.{BasicProcessState, Completed, NotStarted, Running}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq}
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.ChangeResponsePriority.High
import utopia.flow.operator.MaybeEmpty
import utopia.flow.time.Duration
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.result.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.async.{Volatile, VolatileFlag}
import utopia.flow.view.template.eventful.{Changing, Flag}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object ActionQueue
{
	// OTHER    ------------------------
	
	/**
	 * Creates a new action queue
	 * @param maxWidth Maximum width of this queue.
	 *                 I.e. the maximum number of parallel actions at any time.
	 *                 Default = 1.
	 * @param log Implicit logging implementation
	 * @param exc Implicit execution context
	 * @return A new action queue
	 */
	def apply(maxWidth: Int = 1)(implicit log: Logger, exc: ExecutionContext): ActionQueue = {
		if (maxWidth == 1)
			new LinearActionQueue()
		else if (maxWidth < 0)
			throw new IllegalArgumentException(s"Action queue's maximum width can't be $maxWidth")
		else
			new _ActionQueue(maxWidth)
	}
	
	
	// NESTED   ------------------------
	
	object QueuedAction
	{
		// IMPLICIT --------------------
		
		implicit def autoAccessFuture[A](a: QueuedAction[A]): Future[A] = a.future
		
		
		// OTHER    --------------------
		
		/**
		 * @param result Acquired action result
		 * @tparam A type of the action result
		 * @return An action which has already been completed
		 */
		def completed[A](result: A): QueuedAction[A] = new CompletedAction[A](result)
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
		
		
		// IMPLEMENTED  -------------------
		
		override def toString: String = s"Action[$state]"
		
		
		// OTHER    -----------------------
		
		/**
		 * Blocks until this action has finished running
		 * @param timeout Maximum time to wait. Default = infinite.
		 * @return Action result. Failure if timeout was reached or if the process threw.
		 */
		def waitFor(timeout: Duration = Duration.infinite) = future.waitFor(timeout)
		/**
		 * Blocks until this action has started running.
		 * @param timeout Maximum wait timeout. Default = infinite.
		 * @return This action if wait succeeded. Failure if timeout was reached.
		 */
		def waitUntilStarted(timeout: Duration = Duration.infinite) =
			startFuture.waitFor(timeout).map { _ => this }
	}
	
	/**
	 * Common trait for asynchronous processing units that fill the queue's active slots
	 */
	private trait ProcessorBlock
	{
		/**
		 * @return A future that resolves once this process has completed
		 */
		def completionFuture: Future[Any]
		/**
		 * @return Whether this process has completed
		 */
		def isComplete: Boolean
		
		/**
		 * @return A string describing this processor's state
		 */
		def debugString: String
		
		/**
		 * Adds a listener to an asynchronous action.
		 * This method must only be called for asynchronously completing processes.
		 *
		 * @param listener A listener that may be informed of this process' completion
		 * @return True if this process had already completed, and no listener was therefore added.
		 */
		def addCompletionListener(listener: ChangeListener[BasicProcessState]): Boolean
	}
	
	private class CompletedAction[+A](result: A) extends QueuedAction[A]
	{
		// ATTRIBUTES   ----------------------------
		
		override val state = Completed
		override lazy val statePointer = Fixed(Completed)
		
		override lazy val future: Future[A] = Future.successful(result)
		override lazy val startFuture: Future[Unit] = Future.unit
		
		
		// IMPLEMENTED  ----------------------------
		
		override def waitFor(timeout: Duration) = Success(result)
		override def waitUntilStarted(timeout: Duration) = Success(this)
	}
	
	/**
	 * Common trait for action implementations that expose functions to the parent queue
	 * @tparam A Type of result yielded by this action.
	 */
	private trait InteractiveAction[+A] extends QueuedAction[A] with Runnable with ProcessorBlock
	{
		// ABSTRACT --------------------------
		
		/**
		 * @return Whether this action will run and complete asynchronously.
		 *         False if this is a synchronous, i.e. a blocking action.
		 */
		def isAsynchronous: Boolean
		
		
		// COMPUTED -------------------------
		
		/**
		 * @return Whether this action is synchronous and blocks during the [[run]] function.
		 */
		def isSynchronous = !isAsynchronous
		
		
		// IMPLEMENTED  --------------------
		
		override def completionFuture: Future[Any] = future
	}
	
	private object SyncAction
	{
		def apply[A](f: => A)(implicit log: Logger) = new SyncAction[A](_.complete(Try(f)))
		def trying[A](f: => Try[A])(implicit log: Logger) = new SyncAction[A](_.complete(Try(f).flatten))
	}
	/**
	 * A synchronously completing (blocking) action wrapper
	 * @param complete A function that accepts a promise to complete, and completes it synchronously
	 * @param log Implicit logging implementation
	 * @tparam A Type of result yielded by this action.
	 */
	private class SyncAction[A](complete: Promise[A] => Unit)(implicit log: Logger) extends InteractiveAction[A]
	{
		// ATTRIBUTES   ------------------
		
		override val isAsynchronous: Boolean = false
		private val promise = Promise[A]()
		
		private val _statePointer = Volatile.eventful[BasicProcessState](NotStarted)
		override lazy val statePointer: Changing[BasicProcessState] = _statePointer.readOnly
		
		override lazy val startFuture: Future[Unit] =
			_statePointer.findMapFuture { state => if (state.hasStarted) Some(()) else None }
		
		
		// IMPLEMENTED  ------------------
		
		override def future: Future[A] = promise.future
		override def isComplete: Boolean = promise.isCompleted
		override def state = _statePointer.value
		
		override def debugString: String = s"$state (sync)"
		
		override def run() = complete(promise)
		
		// This method is left unimplemented on purpose. It is only used with asynchronously completing actions.
		override def addCompletionListener(listener: ChangeListener[BasicProcessState]): Boolean =
			throw new NotImplementedError("Completion-listening is not available for synchronous actions")
	}
	
	private class AsyncAction[A](start: => Future[A])(implicit log: Logger, exc: ExecutionContext)
		extends InteractiveAction[A]
	{
		// ATTRIBUTES   ----------------
		
		override val isAsynchronous: Boolean = true
		
		/**
		 * Contains:
		 *      - Some(Left) if initialized before the wrapped action was started
		 *      - Some(Right) if initialized after the wrapped action was started
		 *      - None until initialized
		 */
		private val wrappedPointer = Volatile.eventful.optional[Either[Promise[A], Future[A]]]()
		/**
		 * A lazily initialized pointer for tracking action state
		 */
		private val lazyStatePointer = Lazy {
			wrappedPointer.value match {
				// Case: This action has not yet started, but a promise has already been prepared
				//       => Listens to process start & completion, updating the pointer
				case Some(Left(promise)) =>
					val pointer = Pointer.lockable[BasicProcessState](NotStarted)
					startFuture.onComplete { _ =>
						pointer.value = Running
						promise.future.onComplete { _ =>
							pointer.value = Completed
							pointer.lock()
						}
					}
					pointer
				// Case: This action has already started
				case Some(Right(future)) =>
					// Case: This action has completed => Yields a fixed pointer
					if (future.isCompleted)
						Fixed(Completed)
					// Case: This action is still running => Listens to the future completion, updating the pointer
					else {
						val pointer = Pointer.lockable[BasicProcessState](Running)
						future.onComplete { _ =>
							pointer.value = Completed
							pointer.lock()
						}
						pointer
					}
				// Case: No promise or future has been prepared yet => Waits until one is prepared
				case None =>
					val pointer = Volatile.lockable[BasicProcessState](NotStarted)
					wrappedPointer.onceNotEmpty {
						// Case: Prepared as a promise => Listens to process start & completion (WET WET)
						case Left(promise) =>
							startFuture.onComplete { _ =>
								pointer.value = Running
								promise.future.onComplete { _ =>
									pointer.value = Completed
									pointer.lock()
								}
							}
						// Case: Prepared as the final future => Listens to its completion
						case Right(future) =>
							// Case: Already completed => Finalizes the pointer
							if (future.isCompleted) {
								pointer.value = Completed
								pointer.lock()
							}
							// Case: Not yet resolved => Updates once the future resolves
							else {
								pointer.value = Running
								future.onComplete { _ =>
									pointer.value = Completed
									pointer.lock()
								}
							}
					}
					pointer
			}
		}
		
		/**
		 * A lazily initialized future of this action's completion
		 */
		private val lazyFuture = Lazy {
			// Prepares a future if one hasn't been prepared already
			wrappedPointer.mutate {
				// Case: Future already prepared => Returns that
				case Some(wrapped) => wrapped.rightOrMap { _.future } -> Some(wrapped)
				// Case: No future prepared => Forms a promise and returns the future of that promise
				case None =>
					val promise = Promise[A]()
					promise.future -> Some(Left(promise))
			}
		}
		
		override lazy val startFuture: Future[Unit] = wrappedPointer.findMapFuture { _.map { _ => () } }
		
		
		// IMPLEMENTED  ---------------
		
		override def future = lazyFuture.value
		
		override def state = lazyStatePointer.current match {
			// Case: State pointer prepared => Takes the current state from that
			case Some(pointer) => pointer.value
			// Case: No state pointer prepared => Determines the current state using lazyFuture or wrappedPointer
			case None =>
				// Case: State known based on the wrapped future
				if (lazyFuture.current.exists { _.isCompleted })
					Completed
				// Case: State must be determined from wrappedPointer
				else
					wrappedPointer.value match {
						// Case: The process is running or completed
						case Some(Right(future)) => if (future.isCompleted) Completed else Running
						// Case: Not yet started
						case _ => NotStarted
					}
		}
		override def statePointer: Changing[BasicProcessState] = lazyStatePointer.value.readOnly
		
		override def isComplete: Boolean = lazyStatePointer.current match {
			// Case: State pointer tracks the current state => Utilizes that
			case Some(pointer) => pointer.value.isFinal
			case None =>
				lazyFuture.current match {
					// Case: Future available for state-tracking => Uses that
					case Some(future) => future.isCompleted
					// Case: State must be determined from wrappedPointer
					case None => wrappedPointer.value.exists { _.exists { _.isCompleted } }
				}
		}
		
		override def debugString: String = {
			val stateStr = lazyStatePointer.current match {
				case Some(stateP) => s", State: ${stateP.value}"
				case None => ""
			}
			val futureStr = lazyFuture.current match {
				case Some(future) => s", Future (${ future.currentResult })"
				case None => ""
			}
			val wrappedStr = wrappedPointer.value match {
				case None => "Uninitialized"
				case Some(Left(promise)) => s"Promise (${ promise.isCompleted })"
				case Some(Right(future)) => s"Future (${ future.currentResult })"
			}
			s"$wrappedStr$stateStr$futureStr"
		}
		
		override def run() = wrappedPointer.update {
			// Case: Promise already prepared => Prepares to complete that promise with this new future
			case Some(Left(promise)) =>
				Try(start) match {
					case Success(future) =>
						promise.completeWith(future)
						Some(Right(future))
					case Failure(error) =>
						promise.failure(error)
						Some(Right(promise.future))
				}
			// Case: Future already prepared (shouldn't be) => Returns that future
			case Some(Right(future)) => Some(Right(future))
			// Case: No promise prepared => Skips the promise-creation and wraps the new future
			case None => Some(Right(Try(start).getOrMap(Future.failed)))
		}
		
		override def addCompletionListener(listener: ChangeListener[BasicProcessState]): Boolean = {
			if (isComplete)
				true
			else {
				lazyStatePointer.value.addListenerAndSimulateEvent(NotStarted)(listener)
				false
			}
		}
	}
	
	private abstract class AbstractActionQueue(implicit override val exc: ExecutionContext, override val log: Logger)
		extends ActionQueue
	{
		// ATTRIBUTES	------------------
		
		/**
		 * Queues the pending (unstarted) actions
		 */
		protected val queue = Volatile.eventful.seq[InteractiveAction[_]]()
		
		private val lazyQueueSizeP = Lazy { queue.readOnly.map { _.size } }
		// Uses the queue size -pointer, if it has been initialized
		override lazy val notPendingFlag: Flag = lazyQueueSizeP.current match {
			case Some(sizeP) => sizeP.lightMap { _ == 0 }: Flag
			case None => queue.emptyFlag
		}
		override lazy val pendingFlag: Flag = !notPendingFlag
		
		private val _emptyFlag = VolatileFlag.apply(initialState = true)
		
		/**
		 * A change listener used for updating the processor(s), once a process completes
		 */
		protected val updateProcessorsListener = ChangeListener[BasicProcessState] { e =>
			// TODO: Review, which priority would be appropriate here
			if (e.newValue.isFinal)
				Detach.and(High) { _emptyFlag.value = !updateProcessors() }
			else
				Continue
		}
		
		
		// ABSTRACT -----------------------
		
		/**
		 * Makes sure the queue is not left untended
		 * @return Whether at least one process remains active
		 */
		protected def updateProcessors(): Boolean
		
		
		// IMPLEMENTED  -------------------
		
		override def isEmpty: Boolean = _emptyFlag.value
		override def emptyFlag: Flag = _emptyFlag.view
		
		override def containsPendingActions = queue.nonEmpty
		
		override def pendingCount: Int = lazyQueueSizeP.current match {
			case Some(sizeP) => sizeP.value
			case None => queue.value.size
		}
		override def pendingCountPointer = lazyQueueSizeP.value
		
		override protected def _push[A](action: InteractiveAction[A], prepend: Boolean) = {
			// Pushes or prepends the action to queue
			if (prepend) queue.update { action +: _ } else queue :+= action
			// Starts additional processors, if applicable
			_emptyFlag.value = !updateProcessors()
			
			action
		}
		override protected def _pushAll[A](actions: IterableOnce[InteractiveAction[A]], prepend: Boolean) = {
			val _actions = OptimizedIndexedSeq.from(actions)
			if (_actions.isEmpty)
				Empty
			else {
				queue.update { q => if (prepend) _actions ++ q else q ++ _actions }
				_emptyFlag.value = !updateProcessors()
				_actions
			}
		}
	}
	
	/**
	 * An implementation optimized for the width=1 use-case
	 */
	private class LinearActionQueue(implicit exc: ExecutionContext, log: Logger)
		extends AbstractActionQueue
	{
		// ATTRIBUTES   ------------------------
		
		override val maxWidth = 1
		
		/**
		 * Contains an asynchronously completing process that resolves either
		 * one async action or n consecutive synchronous actions
		 */
		private val processorP = Volatile.empty[ProcessorBlock]
		
		
		// IMPLEMENTED  -----------------------
		
		override def hasCapacity = processorP.value.forall { _.isComplete }
		override def currentWidth: Int = processorP.value.count { !_.isComplete }
		
		override def debugString: String = processorP.value match {
			case Some(processor) => s"Processor: {${ processor.debugString }}, queued: $pendingCount"
			case None => s"No processor, queued: $pendingCount"
		}
		
		
		// OTHER    ---------------------------
		
		/**
		 * Makes sure there's an active processor running, if needed.
		 */
		@tailrec
		protected final def updateProcessors(): Boolean = {
			val (newProcessor, activeProcessor) = processorP.mutate { process =>
				// Checks the existing process, whether it's active or completed
				val active = process.filterNot { _.isComplete }
				// Case: The current process is still active => No change
				if (active.isDefined)
					(None, active) -> active
				// Case: No active process => Dequeues an action to process next
				else
					queue.pop() match {
						case Some(nextAction) =>
							// Wraps the action into a synchronously chaining processor, if applicable
							val processor = {
								if (nextAction.isAsynchronous)
									nextAction
								else
									new SyncActionExecutor(queue, nextAction)
							}
							(Some(processor), Some(processor)) -> Some(processor)
						
						// Case: No more actions remain => No process is initiated
						case None => (None, None) -> None
					}
			}
			// Starts the newly created processor, if applicable
			newProcessor.foreach { _.run() }
			// Prepares to update the processor again once the current action completes, if appropriate
			activeProcessor match {
				case Some(active) =>
					// Case: The active process resolved immediately
					//       => Applies recursion to process the next queued item
					if (active.addCompletionListener(updateProcessorsListener))
						updateProcessors()
					else
						true
				
				case None => false
			}
		}
	}
	
	private class _ActionQueue(override val maxWidth: Int)(implicit exc: ExecutionContext, log: Logger)
		extends AbstractActionQueue
	{
		// ATTRIBUTES	------------------
		
		/**
		 * Contains up to [[maxWidth]] (asynchronous) processors at a time.
		 */
		private val processorsP = Volatile.seq[ProcessorBlock]()
		
		
		// IMPLEMENTED  -------------------
		
		override def hasCapacity = !processorsP.value.existsCount(maxWidth) { !_.isComplete }
		override def currentWidth: Int = processorsP.value.count { !_.isComplete }
		
		override def debugString: String = s"Processors: [${
			processorsP.value.iterator.map { p => s"{${ p.debugString }}" }.mkString(", ") }], queued: $pendingCount"
		
		
		// OTHER	----------------------
		
		/**
		 * Makes sure the queue is not left untended
		 */
		@tailrec
		protected final def updateProcessors(): Boolean = {
			// Updates the current processors
			val (newProcessors, processors) = processorsP.mutate { processors =>
				// Removes completed processes
				val remaining = processors.filterNot { _.isComplete }
				val availableCapacity = maxWidth - remaining.size
				
				// If there are queued actions and enough capacity,
				// prepares new processes for those actions
				val newProcessors = {
					// Case: No capacity available => No new processors
					if (availableCapacity <= 0)
						Empty
					else
						queue.pop(availableCapacity).map { a =>
							// Wraps the action into a synchronously chaining process, if appropriate
							if (a.isAsynchronous) a else new SyncActionExecutor(queue, a)
						}
				}
				// Appends the new processors
				val updated = if (newProcessors.isEmpty) remaining else remaining ++ newProcessors
				
				(newProcessors, updated) -> updated
			}
			// Starts the new processors, if applicable
			newProcessors.foreach { _.run() }
			
			// Case: Reached the maximum capacity while there are queued items waiting to be processed
			//       => Listens to process completions in order to add the queued items ASAP
			if (processors.hasSize >= maxWidth && queue.nonEmpty) {
				// Case: A processor became immediately free => Applies recursion
				if (processors.exists { _.addCompletionListener(updateProcessorsListener) })
					updateProcessors()
				// Case: All processors are pending (and now being listened to)
				else
					true
			}
			else
				processors.headOption match {
					// Case: Listening is only needed for updating the empty state
					//       => Listens to the first process only
					case Some(firstProcess) =>
						firstProcess.addCompletionListener(updateProcessorsListener)
						true
						
					// Case: No listening was needed => Returns whether idle (confirms)
					case None => queue.nonEmpty || processorsP.nonEmpty
				}
		}
	}
	
	/**
	 * Chains synchronous actions, emptying the queue.
	 * Stops processing if any asynchronous actions are encountered.
	 * @param queue A pending action queue
	 * @param firstAction The first action to process
	 * @param log Implicit logging implementation
	 * @param exc Implicit execution context
	 */
	private class SyncActionExecutor(queue: Pointer[Seq[InteractiveAction[_]]], firstAction: Runnable)
	                                (implicit log: Logger, exc: ExecutionContext) extends ProcessorBlock with Runnable
	{
		// ATTRIBUTES   -------------------------
		
		/**
		 * A pointer for recording this executor's state
		 */
		private val stateP = Pointer.eventful[BasicProcessState](NotStarted)
		
		private val lazyFuture = Lazy {
			// Updates the state
			stateP.value = Running
			// Starts processing the actions asynchronously
			Future {
				// Runs the actions as long as there are some available
				var nextAction: Option[Runnable] = Some(firstAction)
				while (nextAction.isDefined) {
					nextAction.foreach { _.run() }
					// Pulls the next action from the queue, but only if it's synchronous
					nextAction = queue.mutate { q =>
						q.headOption.filter { _.isSynchronous } match {
							case Some(nextAction) => Some(nextAction) -> q.tail
							case None => None -> q
						}
					}
				}
				stateP.value = Completed
			}
		}
		
		
		// IMPLEMENTED  -------------------------
		
		override def isComplete: Boolean = stateP.value.isFinal
		override def completionFuture: Future[Any] = lazyFuture.value
		
		override def debugString: String = {
			val futureStr = lazyFuture.current match {
				case Some(future) => s", Future (${ future.currentResult })"
				case None => ""
			}
			s"${stateP.value}, $futureStr"
		}
		
		override def addCompletionListener(listener: ChangeListener[BasicProcessState]): Boolean = {
			if (isComplete)
				true
			else {
				stateP.addListenerAndSimulateEvent(NotStarted)(listener)
				false
			}
		}
		
		// NB: Assumes that this function is only called once
		override def run() = lazyFuture.value
	}
}

/**
  * Used for running up to n actions in parallel, queueing the remaining actions.
  * @author Mikko Hilpinen
  * @since 22.5.2019, v1.4.1+
  */
trait ActionQueue extends MaybeEmpty[ActionQueue]
{
	// ABSTRACT ----------------------
	
	/**
	 * @return Implicit logging implementation used in pointer-management
	 */
	protected implicit def log: Logger
	/**
	 * @return Implicit execution context used in asynchronous progress-tracking.
	 */
	protected implicit def exc: ExecutionContext
	
	/**
	 * @return Maximum number of parallelly running actions at any one time.
	 */
	def maxWidth: Int
	/**
	 * @return Number of currently running parallel processes
	 */
	def currentWidth: Int
	/**
	 * @return Whether this queue has capacity for more actions right now
	 */
	def hasCapacity: Boolean
	
	/**
	 * @return Number of actions that are currently waiting to be processed, i.e. have not started yet.
	 */
	def pendingCount: Int
	/**
	 * A pointer that contains the number of queued (waiting) items in this queue at any time.
	 */
	def pendingCountPointer: Changing[Int]
	/**
	 * @return A flag that contains true while there are actions waiting to be started
	 */
	def pendingFlag: Flag
	/**
	 * @return A flag that contains true while there are no actions waiting to be started.
	 *         Note: This is not the same as having no actions running.
	 * @see [[emptyFlag]]
	 */
	def notPendingFlag: Flag
	/**
	 * @return A flag that contains true while this queue is empty
	 */
	def emptyFlag: Flag
	
	/**
	 * @return A string for debugging the state of this queue
	 */
	def debugString: String
	
	/**
	 * Adds an action to this queue
	 * @param action Action to add
	 * @param prepend Whether to place this action before the pending actions
	 * @tparam A Type of the action's completion results
	 * @return The specified action
	 */
	protected def _push[A](action: InteractiveAction[A], prepend: Boolean = false): QueuedAction[A]
	/**
	 * Adds n actions to this queue
	 * @param actions Actions to add
	 * @param prepend Whether to place these actions before the currently pending actions
	 * @tparam A Type of the actions' completion results
	 * @return The specified actions
	 */
	protected def _pushAll[A](actions: IterableOnce[InteractiveAction[A]], prepend: Boolean = false): Seq[QueuedAction[A]]
	
	
	// COMPUTED ----------------------
	
	/**
	 * @return Whether the queue part of this
	 */
	def containsPendingActions = pendingFlag.value
	/**
	 * @return Whether this queue is currently processing actions at maximum capacity
	 */
	def isFull = !hasCapacity
	
	/**
	 * @return A future that resolves once this queue is completely empty,
	 *         i.e. once all the queued actions have fully resolved.
	 */
	def emptyFuture: Future[Any] = emptyFlag.future
	/**
	 * @return A future that resolves once no action in this queue is waiting to be started.
	 *         Note: This doesn't mean that all actions have completed yet.
	 * @see [[emptyFuture]]
	 */
	def notPendingFuture = notPendingFlag.future
	
	/**
	  * @return An execution context that uses this action queue
	  */
	def asExecutionContext: ExecutionContext = QueuedExecutionContext
	
	@deprecated("Renamed to .pendingCount", "v2.8")
	def queueSize: Int = pendingCount
	@deprecated("Renamed to .pendingCountPointer", "v2.8")
	def queueSizePointer: Changing[Int] = pendingCountPointer
	
	
	// IMPLEMENTED  ------------------
	
	override def self: ActionQueue = this
	
	
	// OTHER	----------------------
	
	/**
	  * Pushes a new operation to the end of this queue
	  * @param operation An operation
	  * @tparam A Operation result type
	  * @return The queued action
	  */
	def push[A](operation: => A): QueuedAction[A] = _push(SyncAction(operation))
	/**
	  * Pushes a new operation to the end of this queue
	  * @param operation An operation that returns a Try
	  * @tparam A Operation result type (inside try)
	  * @return The queued action
	  */
	def pushTry[A](operation: => Try[A]) = _push(SyncAction.trying(operation))
	/**
	  * Pushes a new operation to the end of this queue
	  * @param resolvesAsync An operation that completes asynchronously, returning a Future.
	  * @tparam A Type of future the operation resolves into
	  * @return The queued action
	  */
	def pushAsync[A](resolvesAsync: => Future[A]) = _push(new AsyncAction[A](resolvesAsync))
	
	/**
	 * Appends n actions to this queue
	 * @param operations Actions to add to this queue
	 * @tparam A Type of individual operation results
	 * @return Actions that were added
	 */
	def pushAll[A](operations: IterableOnce[View[A]]): Seq[QueuedAction[A]] =
		_pushAll(operations.iterator.map { v => SyncAction { v.value } })
	/**
	 * Appends n actions to this queue
	 * @param operations Actions to add to this queue. Each yields a Try.
	 * @tparam A Type of individual operation results
	 * @return Actions that were added
	 */
	def pushAllTries[A](operations: IterableOnce[View[Try[A]]]) =
		_pushAll(operations.iterator.map { v => SyncAction.trying { v.value } })
	/**
	 * Appends n actions to this queue
	 * @param operations Actions to add to this queue. Each yields a Future.
	 * @tparam A Type of individual operation results
	 * @return Actions that were added
	 */
	def pushAllAsync[A](operations: IterableOnce[View[Future[A]]]) =
		_pushAll(operations.iterator.map { v => new AsyncAction[A](v.value) })
	
	/**
	 * Prepends n actions to the beginning of this queue
	 * @param operations Actions to add to this queue
	 * @tparam A Type of individual operation results
	 * @return Actions that were added
	 */
	def prependAll[A](operations: IterableOnce[() => A]): Seq[QueuedAction[A]] =
		_pushAll(operations.iterator.map { a => SyncAction { a() } }, prepend = true)
	/**
	 * Prepends n actions to the beginning of this queue
	 * @param operations Actions to add to this queue. Each yields a Try.
	 * @tparam A Type of individual operation results
	 * @return Actions that were added
	 */
	def prependAllTries[A](operations: IterableOnce[() => Try[A]]) =
		_pushAll(operations.iterator.map { a => SyncAction.trying { a() } }, prepend = true)
	/**
	 * Prepends n actions to the beginning of this queue
	 * @param operations Actions to add to this queue. Each yields a Future.
	 * @tparam A Type of individual operation results
	 * @return Actions that were added
	 */
	def prependAllAsync[A](operations: IterableOnce[() => Future[A]]) =
		_pushAll(operations.iterator.map { a => new AsyncAction[A](a()) }, prepend = true)
	
	/**
	  * Pushes a new operation to this queue. Passes all other operations that have been queued.
	  * @param operation An operation
	  * @tparam A Operation result type
	  * @return The action that was queued
	  */
	def prepend[A](operation: => A): QueuedAction[A] = _push(SyncAction[A](operation), prepend = true)
	/**
	  * Pushes a new operation to the end of this queue. Passes all other operations that have been queued.
	  * @param resolvesAsync An operation that completes asynchronously, returning a Future.
	  * @tparam A Type of future the operation resolves into
	  * @return The action that was queued
	  */
	def prependAsync[A](resolvesAsync: => Future[A]): QueuedAction[A] =
		_push(new AsyncAction[A](resolvesAsync), prepend = true)
	
	
	// NESTED   -----------------------
	
	private object QueuedExecutionContext extends ExecutionContext
	{
		override def execute(runnable: Runnable) = push { runnable.run() }
		override def reportFailure(cause: Throwable) = log(cause)
	}
}

package utopia.flow.async.process

import utopia.flow.async.context.CloseHook
import utopia.flow.async.process.ProcessState.{Cancelled, Completed, Looping, NotStarted, Running, Stopped}
import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.{Volatile, VolatileFlag}
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.template.eventful.Flag

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Process
{
	// OTHER    ----------------------------
	
	/**
	  * Creates a new process based on a function call
	  * @param waitLock Wait lock to use (optional)
	  * @param shutdownReaction How this process should react to system shutdown events
	  *                         (default = cancel pending process)
	  * @param isRestartable Whether this process may be run multiple times (default = true)
	  * @param f Wrapped function. Accepts a pointer which contains whether the function should hurry its completion
	  *          (in case of a jvm shutdown or stop() call)
	  * @param exc Implicit execution context
	  * @param logger Implicit logger for exceptions that reach the process function
	  * @tparam U Arbitrary function result type
	  * @return A new process that uses the underlying function
	  */
	def apply[U](waitLock: AnyRef = new AnyRef, shutdownReaction: ShutdownReaction = Cancel,
	             isRestartable: Boolean = true)
	            (f: => Flag => U)
	            (implicit exc: ExecutionContext, logger: Logger): Process =
		new FunctionProcess[U](waitLock, shutdownReaction, isRestartable)(f)
	
	
	// NESTED   ----------------------------
	
	private class FunctionProcess[U](waitLock: AnyRef, shutdownReaction: ShutdownReaction = Cancel,
	                                 override val isRestartable: Boolean)
	                                (f: => Flag => U)
	                                (implicit exc: ExecutionContext, logger: Logger)
		extends Process(waitLock, Some(shutdownReaction))
	{
		override protected def runOnce() = f(hurryFlag)
	}
}

/**
  * A common abstract class for processes that may be run, cancelled and broken
  * @author Mikko Hilpinen
  * @since 24.2.2022, v1.15
  */
abstract class Process(protected val waitLock: AnyRef = new AnyRef,
                       val shutdownReaction: Option[ShutdownReaction] = None)
                      (implicit exc: ExecutionContext, logger: Logger)
	extends Runnable with Breakable
{
	// ATTRIBUTES   ------------------------------
	
	private val _statePointer = Volatile.eventful[ProcessState](NotStarted)
	private val completionFuturePointer = ResettableLazy { _statePointer.futureWhere { _.isFinal } }
	private val _shutdownFlag = VolatileFlag()
	
	/**
	  * A pointer which indicates whether this process should be hurried
	  * (based on stop & shutdown status + shutdown reaction)
	  */
	lazy val hurryFlag: Flag = statePointer.mergeWith(shutDownFlag) { case (state, shutdown) =>
		state.isBroken || (shutdown && shutdownReaction.exists { _.skipWait })
	}
	
	
	// ABSTRACT ----------------------------------
	
	/**
	  * @return Whether this process may be restarted after it has completed
	  */
	protected def isRestartable: Boolean
	
	/**
	  * Performs this process once synchronously
	  */
	protected def runOnce(): Unit
	
	
	// COMPUTED ----------------------------------
	
	/**
	  * @return Whether this process may be started at this time
	  *         (false if already completed and not restartable, or if currently running)
	  */
	def isStartable = {
		val s = state
		if (s.isFinal)
			isRestartable
		else
			s.isNotRunning
	}
	
	/**
	  * @return A future that completes when this process completes.
	  *         NB: The future may finish immediately if this process has already been completed.
	  */
	def completionFuture = completionFuturePointer.value
	
	/**
	  * @return Whether the JVM hosting this process has scheduled a shutdown during this process' completion
	  */
	def isShutDown = _shutdownFlag.value
	
	/**
	  * @return A flag that contains true after the JVM hosting this process has scheduled a shutdown during
	  *         this process' completion
	  */
	def shutDownFlag = _shutdownFlag.view
	
	@deprecated("Renamed to .hurryFlag", "v2.6")
	def hurryPointer = hurryFlag
	@deprecated("Renamed to .shutDownFlag", "v2.6")
	def shutdownPointer = shutDownFlag
	
	
	// COMPUTED ----------------------------------
	
	/**
	  * @return The current state of this wait instance
	  */
	def state = _statePointer.value
	/**
	  * @return A pointer that holds this wait instance's state
	  */
	def statePointer = _statePointer.readOnly
	
	/**
	  * @return Whether this process should hurry to complete itself
	  */
	protected def shouldHurry = hurryFlag.value
	
	
	// IMPLEMENTED  ------------------------------
	
	override def stop() = {
		val shouldWait = _statePointer.mutate { currentState =>
			if (currentState.isFinal)
				false -> currentState
			else
				true -> currentState.broken
		}
		if (shouldWait) {
			WaitUtils.notify(waitLock)
			completionFuture
		}
		else
			Future.successful(state)
	}
	
	override def run() = {
		// Updates the state and checks whether this process may actually be run
		val shouldRun = _statePointer.mutate { currentState =>
			// Case: Cancelled => May cancel the cancellation, but doesn't perform this iteration
			if (currentState == Cancelled)
				false -> (if (isRestartable) NotStarted else currentState)
			// Case: Already running => ignores this call
			else if (currentState.isRunning)
				false -> currentState
			// Case: Startable or restartable => starts
			else if (currentState.hasNotStarted || isRestartable)
				true -> Running
			// Case: Already completed and can't restart => doesn't budge
			else
				false -> currentState
		}
		if (shouldRun) {
			// Prepares a new completion future if necessary
			completionFuturePointer.filterNot { _.isCompleted }
			// Prepares for possible jvm shutdown
			_shutdownFlag.reset()
			val reaction = shutdownReaction.map { new ShutDownAction(_) }
			reaction.foreach { CloseHook += _ }
			// Runs this process (catches and prints errors)
			Iterator.continually {
				Try { runOnce() }.failure.foreach { logger(_, "Asynchronous process threw an uncatched exception") }
				// Updates the state afterwards
				_statePointer.updateAndGet { currentState =>
					// For looping processes, continues one more run
					if (currentState == Looping)
						Running
					else if (currentState.isBroken)
						Stopped
					else
						Completed
				}
			}.takeWhile { _.isNotFinal }.foreach { _ => () }
			reaction.foreach { CloseHook -= _ }
		}
	}
	
	override def registerToStopOnceJVMCloses() = {
		// The Cancel -ShutdownReaction already handles this feature, if used
		if (!shutdownReaction.contains(Cancel))
			super.registerToStopOnceJVMCloses()
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * Stops this process, but only if it is running
	  * @return A future that completes when the stop is finalized. Will contain the resulting state.
	  */
	def stopIfRunning() = {
		if (state.isRunning)
			stop()
		else
			Future.successful(state)
	}
	
	/**
	  * Runs this process asynchronously, unless running already
	  */
	def runAsync(loopIfRunning: Boolean = false) = {
		// Checks whether this process should run. Applies the looping, if necessary
		val shouldRun = _statePointer.mutate { state =>
			// Case: Final state => reruns if allowed
			if (state.isFinal)
				isRestartable -> state
			// Case: Not started => starts
			else if (state.isNotRunning)
				true -> state
			// Case: Running => loops or skips
			else
				false -> (if (loopIfRunning) Looping else state)
		}
		if (shouldRun)
			exc.execute(this)
	}
	
	/**
	  * Adds a delay to this process
	  * @param wait Wait to perform before this process is run
	  * @return The delayed wrapper process
	  */
	def delayed(wait: WaitTarget) = {
		DelayedProcess(wait, shutdownReaction = shutdownReaction) { hurryPointer =>
			hurryPointer.addListener { e =>
				if (e.newValue) {
					markAsInterrupted()
					Detach
				}
				else
					Continue
			}
			run()
		}
	}
	
	/**
	  * Runs this process asynchronously, after a delay
	  * @param wait Wait to perform before this process is run
	  */
	def runAsyncAfter(wait: WaitTarget) = delayed(wait).runAsync()
	
	/**
	  * Marks this process's state as having been interrupted.
	  * This resembles calling stop(), but only modifies the state of this process,
	  * skipping the additional asynchronous operations.
	  *
	  * This function should be called in situations where the process finds out its broken during runOnce().
	  * For example, if an InterruptedException is encountered (while waiting),
	  * this process should call this function and then hurry to complete the operation it was performing.
	  *
	  * @return Whether this process was successfully marked as broken.
	  *         False if this process had already completed when this function was called.
	  */
	protected def markAsInterrupted() = _statePointer.mutate { oldState =>
		if (oldState.isFinal) false -> oldState else true -> oldState.broken
	}
	
	
	// NESTED   ---------------------------
	
	private class ShutDownAction(reaction: ShutdownReaction) extends Breakable
	{
		override def stop() = {
			_shutdownFlag.set()
			if (reaction.finishBeforeShutdown) {
				if (reaction.skipWait)
					WaitUtils.notify(waitLock)
				completionFuture
			}
			else
				Process.this.stop()
		}
	}
}

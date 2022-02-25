package utopia.flow.async

import utopia.flow.async.ProcessState.{Cancelled, Completed, NotStarted, Running, Stopped}
import utopia.flow.async.ShutdownReaction.Cancel
import utopia.flow.datastructure.mutable.ResettableLazy
import utopia.flow.event.ChangingLike
import utopia.flow.time.{WaitTarget, WaitUtils}
import utopia.flow.util.CollectionExtensions._

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

object Process
{
	// OTHER    ----------------------------
	
	/**
	  * Creates a new process based on a function call
	  * @param waitLock Wait lock to use (optional)
	  * @param isRestartable Whether this process may be run multiple times (default = true)
	  * @param f Wrapped function. Accepts a pointer which contains whether the function should hurry its completion
	  *          (in case of a jvm shutdown or stop() call)
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary function result type
	  * @return A new process that uses the underlying function
	  */
	def apply[U](waitLock: AnyRef = new AnyRef, isRestartable: Boolean = true)
	            (f: => ChangingLike[Boolean] => U)
	            (implicit exc: ExecutionContext): Process =
		new FunctionProcess[U](waitLock, isRestartable)(f)
	
	
	// NESTED   ----------------------------
	
	private class FunctionProcess[U](waitLock: AnyRef, override val isRestartable: Boolean)
	                                (f: => ChangingLike[Boolean] => U)
	                                (implicit exc: ExecutionContext)
		extends Process(waitLock, Some(Cancel))
	{
		override protected def runOnce() = f(hurryPointer)
	}
}

/**
  * A common abstract class for processes that may be run, cancelled and broken
  * @author Mikko Hilpinen
  * @since 24.2.2022, v1.15
  */
abstract class Process(protected val waitLock: AnyRef = new AnyRef,
                       val shutdownReaction: Option[ShutdownReaction] = None)
                      (implicit exc: ExecutionContext)
	extends Runnable with Breakable
{
	// ATTRIBUTES   ------------------------------
	
	private val _statePointer = Volatile[ProcessState](NotStarted)
	private val completionFuturePointer = ResettableLazy { _statePointer.futureWhere { _.isFinal } }
	private val _shutdownPointer = VolatileFlag()
	
	/**
	  * A pointer which indicates whether this process should be hurried
	  * (based on stop & shutdown status + shutdown reaction)
	  */
	lazy val hurryPointer = statePointer.mergeWith(shutdownPointer) { case (state, shutdown) =>
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
	def isShutDown = _shutdownPointer.value
	
	/**
	  * @return A flag that contains true after the JVM hosting this process has scheduled a shutdown during
	  *         this process' completion
	  */
	def shutdownPointer = _shutdownPointer.valueView
	
	
	// COMPUTED ----------------------------------
	
	/**
	  * @return The current state of this wait instance
	  */
	def state = _statePointer.value
	/**
	  * @return A pointer that holds this wait instance's state
	  */
	def statePointer = _statePointer.valueView
	
	/**
	  * @return Whether this process should hurry to complete itself
	  */
	protected def shouldHurry = hurryPointer.value
	
	
	// IMPLEMENTED  ------------------------------
	
	override def stop() = {
		val shouldWait = _statePointer.pop { currentState =>
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
	
	override def run() =
	{
		// Updates the state and checks whether this process may actually be run
		val shouldRun = _statePointer.pop { currentState =>
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
			_shutdownPointer.reset()
			val reaction = shutdownReaction.map { new ShutDownAction(_) }
			reaction.foreach { CloseHook += _ }
			// Runs this process (catches and prints errors)
			Try { runOnce() }.failure.foreach { error =>
				System.err.println(s"$this process failed")
				error.printStackTrace()
			}
			// Updates the state afterwards
			_statePointer.update { currentState =>
				if (currentState.isBroken) Stopped else Completed
			}
			reaction.foreach { CloseHook -= _ }
		}
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
	  * Runs this process asynchronously
	  */
	def runAsync() = {
		if (isStartable)
			exc.execute(this)
	}
	
	/**
	  * Adds a delay to this process
	  * @param wait Wait to perform before this process is run
	  * @return The delayed wrapper process
	  */
	def delayed(wait: WaitTarget) = {
		DelayedProcess(wait, shutdownReaction = shutdownReaction) { hurryPointer =>
			hurryPointer.addListener { e => if (e.newValue) stop() }
			run()
		}
	}
	
	/**
	  * Runs this process asynchronously, after a delay
	  * @param wait Wait to perform before this process is run
	  */
	def runAsyncAfter(wait: WaitTarget) = delayed(wait).runAsync()
	
	
	// NESTED   ---------------------------
	
	private class ShutDownAction(reaction: ShutdownReaction) extends Breakable
	{
		override def stop() = {
			_shutdownPointer.set()
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

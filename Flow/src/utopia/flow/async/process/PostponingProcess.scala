package utopia.flow.async.process

import utopia.flow.async.process.ProcessState.Completed
import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.async.process.WaitTarget.{Until, UntilNotified}
import utopia.flow.collection.immutable.range.HasEnds
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.UncertainBoolean.{Certain, Uncertain}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.{VolatileFlag, VolatileOption}
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

object PostponingProcess
{
	// OTHER    ------------------------------
	
	/**
	  * Creates a new process instance that reacts to changes in the specified wait target pointer by changing the
	  * process delay and possibly by resetting itself.
	  * @param targetPointer A pointer that contains the currently active wait target
	  * @param waitLock A wait lock this process should use (default = new wait lock)
	  * @param shutdownReaction How this process should react to JVM shutdowns (default = no reaction)
	  * @param isRestartable Whether this process should
	  *                      a) Be restartable manually AND
	  *                      b) Restart itself when the target pointer updates after this process has completed
	  *                      True by default
	  * @param f A function to perform once an active wait target has been reached
	  * @param exc Implicit execution context
	  * @param logger Implicit logger (logs errors thrown by the specified function 'f')
	  * @tparam U Arbitrary function result type
	  * @return A new process ready to be started (i.e. not yet started)
	  */
	def apply[U](targetPointer: Changing[WaitTarget], waitLock: AnyRef = new AnyRef,
	             shutdownReaction: Option[ShutdownReaction] = None, isRestartable: Boolean = true)
	            (f: Changing[Boolean] => U)
	            (implicit exc: ExecutionContext, logger: Logger): PostponingProcess =
		new FunctionalPostponingProcess(targetPointer, waitLock, shutdownReaction, isRestartable)(f)
	
	/**
	  * Creates a new process that delays the specified action by a certain amount each time .runAsync() is called,
	  * so that the action will only be performed once the process has not been touched after a while
	  * (or the specified maximum time threshold is reached)
	  * @param delayRange The delay between the first .runAsync() call and the actual process completion
	  * @param shutDownReaction How this process should react to JVM shutdown. Default = cancel the process.
	  * @param isRestartable Whether this process should be able to be run more than once (default = true)
	  * @param action The action performed after the delay has completed
	  * @param exc Implicit execution context
	  * @param log Implicit logging implementation
	  * @tparam U Arbitrary action result type
	  * @return A new process
	  */
	def by[U](delayRange: HasEnds[FiniteDuration], shutDownReaction: ShutdownReaction = Cancel,
	          isRestartable: Boolean = true)
	         (action: => U)
	         (implicit exc: ExecutionContext, log: Logger): Process =
	{
		// Case: No delay has been requested => Uses a non-delayed process
		if (delayRange.start <= Duration.Zero)
			Process(shutdownReaction = shutDownReaction, isRestartable = isRestartable) { _ => action }
		// Case: There is no difference between the minimum and maximum delay => Uses a delayed process
		else if (delayRange.start >= delayRange.end)
			DelayedProcess(delayRange.start, shutdownReaction = Some(shutDownReaction),
				isRestartable = isRestartable) { _ => action }
		// Case: Default => Uses a postponing process
		else
			new RangePostponingRevalidationProcess(new PointerWithEvents[WaitTarget](UntilNotified),
				delayRange.start, delayRange.end, Some(shutDownReaction), isRestartable)(action)
	}
	
	
	// NESTED   ----------------------------
	
	private class FunctionalPostponingProcess[U](targetPointer: Changing[WaitTarget], waitLock: AnyRef,
	                                             shutdownReaction: Option[ShutdownReaction],
	                                             override val isRestartable: Boolean)
	                                            (f: Changing[Boolean] => U)
	                                            (implicit exc: ExecutionContext, logger: Logger)
		extends PostponingProcess(targetPointer, waitLock, shutdownReaction)
	{
		override protected def afterDelay() = f(hurryPointer)
	}
	
	private class RangePostponingRevalidationProcess(waitTargetPointer: PointerWithEvents[WaitTarget],
	                                                 minRevalidationDelay: FiniteDuration,
	                                                 maxRevalidationDelay: FiniteDuration,
	                                                 shutdownReaction: Option[ShutdownReaction],
	                                                 override val isRestartable: Boolean)
	                                                (action: => Unit)
	                                                (implicit exc: ExecutionContext, log: Logger)
		extends PostponingProcess(waitTargetPointer, shutdownReaction = shutdownReaction)
	{
		// ATTRIBUTES   -----------------
		
		// Contains the latest allowed update time, which is first unfulfilled update request time + max wait duration
		private val latestUpdateTimePointer = VolatileOption[Instant]()
		
		
		// IMPLEMENTED  -----------------
		
		override protected def afterDelay(): Unit = {
			// Resets the update request time now that the update completes
			latestUpdateTimePointer.clear()
			action
		}
		
		
		// OTHER    -------------------
		
		override def runAsync(loopIfRunning: Boolean) = {
			val wasRunning = state.isRunning
			// Delays the run
			val maxUpdateTime = latestUpdateTimePointer.setOneIfEmpty(Now + maxRevalidationDelay)
			waitTargetPointer.value = Until((Now + minRevalidationDelay) min maxUpdateTime)
			// Starts this process, if not running already
			super.runAsync(loopIfRunning && wasRunning)
		}
	}
}

/**
  * An abstract process that may postpone or alter a scheduled completion of a specific function.
  * Works like DelayedProcess, except that the delay may be modified while it is active.
  * @author Mikko Hilpinen
  * @since 12.9.2022, v1.17
  */
abstract class PostponingProcess(waitTargetPointer: Changing[WaitTarget], waitLock: AnyRef = new AnyRef,
                                 shutdownReaction: Option[ShutdownReaction] = None)
                                (implicit exc: ExecutionContext, logger: Logger)
	extends Process(waitLock, shutdownReaction)
{
	// ATTRIBUTES   ------------------------
	
	// True while the wait target was swapped during wait
	private val resetFlag = VolatileFlag()
	
	
	// INITIAL CODE -------------------------
	
	// Reacts to changes in the target wait time once started
	statePointer.onNextChange { _ =>
		waitTargetPointer.addContinuousAnyChangeListener {
			val st = state
			// Case: Running while wait time changes => Updates the wait
			if (st.isRunning) {
				resetFlag.set()
				WaitUtils.notify(waitLock)
			}
			// Case: Finished while wait time changes => Runs this process again with the new wait target
			// (provided that this is allowed by 'isRestartable')
			else if (st == Completed && isRestartable)
				runAsync()
		}
	}
	
	
	// ABSTRACT ----------------------------
	
	/**
	  * This function is called once the delay is over
	  */
	protected def afterDelay(): Unit
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def runOnce() = {
		// Waits until any of:
		// a) A wait target is reached successfully
		// b) This process is broken or scheduled to hurry
		val shouldRun = Iterator.continually {
			// Case: Scheduled to hurry => Skips waiting (may also skip the execution)
			if (shouldHurry)
				Certain(state.isNotBroken)
			else {
				// Next wait target
				val target = waitTargetPointer.value.breakable
				if (target.isPositive) {
					// Waits on the wait target until wait completes or the wait lock is notified
					if (Wait(target, waitLock)) {
						// Case: Scheduled to hurry during waiting => Skips waiting (and possibly execution)
						if (shouldHurry)
							Certain(state.isNotBroken)
						// Case: Wait target was switched during waiting => Starts over with the new wait target
						else if (resetFlag.getAndReset())
							Uncertain
						// Case: Wait target was reached => Moves to execution
						else
							Certain(true)
					}
					// Case: Wait was interrupted with an InterruptedException => Skips wait and execution
					else
						Certain(false)
				}
				// Case: No wait was scheduled => Moves immediately to execution
				else
					Certain(true)
			}
		}.flatMap { _.value }.next()
		// Case: Execution was allowed => Executes
		if (shouldRun) {
			// Executes the wrapped function
			afterDelay()
		}
	}
}

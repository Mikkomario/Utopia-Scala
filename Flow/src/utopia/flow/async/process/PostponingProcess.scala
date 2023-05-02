package utopia.flow.async.process

import utopia.flow.async.process.ProcessState.Completed
import utopia.flow.util.UncertainBoolean.{Certain, Uncertain}
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.VolatileFlag
import utopia.flow.view.template.eventful.Changing

import scala.concurrent.ExecutionContext

object PostponingProcess
{
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

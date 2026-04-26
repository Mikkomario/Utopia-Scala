package utopia.flow.async.process

import utopia.flow.async.process.ShutdownReaction.{Cancel, DelayShutdown, SkipDelay}
import utopia.flow.async.process.WaitTarget.{NoWait, WeeklyTime}
import utopia.flow.time.{Duration, WeekDay}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.View
import utopia.flow.view.template.eventful.Flag

import java.time.LocalTime
import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

object LoopingProcess
{
	// ATTRIBUTES   -------------------------
	
	val factory = LoopingProcessFactory()
	
	
	// IMPLICIT -----------------------------
	
	// Implicitly converts this object into a factory instance
	implicit def objectAsFactory(o: LoopingProcess.type): LoopingProcessFactory = o.factory
	
	
	// OTHER    -----------------------------
	
	/**
	  * Creates a new infinite looping process
	  * @param interval Delay between this loop's iterations
	  * @param waitLock Wait lock to use (optional)
	  * @param waitFirst Whether an interval's amount of time should be waited before running the function the
	  *                  first time (default = false = run the function immediately).
	  * @param isRestartable Whether this loop should be restartable (default = true)
	  * @param f The function that will be called regularly. Accepts a pointer that shows whether the function should
	  *          hurry to complete itself.
	  * @param exc Implicit execution context
	  * @return A new looping process
	  */
	@deprecated("Please use .regularly(Duration) instead", "v2.9")
	def static[U](interval: Duration, waitLock: AnyRef = new AnyRef, waitFirst: Boolean = false,
	              isRestartable: Boolean = true)
	             (f: Flag => U)
	             (implicit exc: ExecutionContext, logger: Logger) =
		LoopingProcessFactory(waitLock = waitLock, isRestartable = isRestartable).regularly(interval, waitFirst)(f)
	
	
	// NESTED   -----------------------------
	
	case class LoopingProcessFactory(startDelayView: Option[View[WaitTarget]] = None,
	                                 waitLock: AnyRef = new AnyRef, shutdownReaction: ShutdownReaction = Cancel,
	                                 isRestartable: Boolean = false, startsImmediately: Boolean = false)
	{
		/**
		 * @return A copy of this factory which starts the created loops immediately
		 */
		def started = copy(startsImmediately = true)
		/**
		 * @return A copy of this factory which allows the loops to repeat once they've finished or been stopped
		 */
		def restartable = copy(isRestartable = true)
		
		/**
		 * @return A copy of this factory that executes the repeated action once before allowing JVM to shut down.
		 *         The delay itself is skipped, however.
		 */
		def executingOnShutdown = withShutdownReaction(SkipDelay)
		/**
		 * @return A copy of this factory that executes the repeated action once before allowing JVM to shut down.
		 *         Full delay will be applied.
		 */
		def delayingShutdown = withShutdownReaction(DelayShutdown)
		
		/**
		 * @param delay Delay to apply before the first run
		 * @return A copy of this factory applying the specified delay before the first loop iteration
		 */
		def after(delay: WaitTarget): LoopingProcessFactory = after(View.fixed(delay))
		/**
		 * @param delayView A view to the delay to apply before the first run
		 * @return A copy of this factory applying the specified delay before the first loop iteration
		 */
		def after(delayView: View[WaitTarget]) = copy(startDelayView = Some(delayView))
		
		/**
		 * @param waitLock Wait lock to use for possibly skipping the loop delay
		 * @return A copy of this loop using the specified wait lock
		 */
		def withWaitLock(waitLock: AnyRef) = copy(waitLock = waitLock)
		
		/**
		 * @param shutdownReaction Reaction to apply to JVM shutdown
		 * @return A copy of this factory applying the specified shutdown reaction
		 */
		def withShutdownReaction(shutdownReaction: ShutdownReaction) =
			copy(shutdownReaction = shutdownReaction)
		
		/**
		 * Creates a new infinite looping process that iterates once every day
		 * @param runTime Time of day when this process should be run
		 * @param andImmediately Whether the first iteration of this loop should be performed immediately
		 *                       (causing the first delay to be less than 24 hours).
		 *                       Default = false = first iteration is at 'runTime' today or tomorrow.
		 * @param f The function that will be called regularly. Accepts a pointer that shows whether the function should
		 *          hurry to complete itself.
		 * @param exc Implicit execution context
		 * @return A new looping process
		 */
		def daily[U](runTime: LocalTime, andImmediately: Boolean = false)(f: Flag => U)
		            (implicit exc: ExecutionContext, logger: Logger) =
			_regularly(runTime, waitFirst = !andImmediately)(f)
		/**
		 * Creates a new infinite looping process that iterates once a week
		 * @param day Weekday on which the function should be called
		 * @param time Time of day at which the function should be called (on targeted weekday)
		 * @param andImmediately Whether the first iteration of this loop should be performed immediately.
		 *                       Default = false.
		 * @param f A function to all once every week. Accepts a pointer that shows whether the function should
		 *          hurry to complete itself.
		 * @param exc Implicit execution context
		 * @tparam U Arbitrary result type
		 * @return A new process that loops weekly
		 */
		def weekly[U](day: WeekDay, time: LocalTime, andImmediately: Boolean = false)(f: Flag => U)
		             (implicit exc: ExecutionContext, logger: Logger) =
			_regularly(WeeklyTime(day, time), waitFirst = !andImmediately)(f)
		
		/**
		 * Creates a new looping process
		 * @param interval Interval between loop iterations
		 * @param waitFirst Whether to wait 'interval' before the first loop iteration.
		 *                  If set to true, overrides any previously specified starting delay.
		 *                  Default = false.
		 * @param f A function to call on each loop iteration.
		 *          Receives a pointer that contains true while this process should hurry to complete itself.
		 * @param exc Implicit execution context
		 * @param log Implicit logging implementation
		 * @return A new looping process.
		 */
		def regularly[U](interval: Duration, waitFirst: Boolean = false)(f: Flag => U)
		                (implicit exc: ExecutionContext, log: Logger) =
			_regularly(interval, waitFirst)(f)
		
		/**
		 * Creates a new looping process
		 * @param f A function to call on each loop iteration.
		 *          Yields the wait target for the next loop iteration.
		 *          Receives a pointer that contains true while this process should hurry to complete itself.
		 * @param exc Implicit execution context
		 * @param log Implicit logging implementation
		 * @return A new looping process.
		 */
		def continuously(f: Flag => WaitTarget)(implicit exc: ExecutionContext, log: Logger) =
			apply { hurryFlag => Some(f(hurryFlag)) }
		/**
		 * Creates a new looping process
		 * @param f A function to call on each loop iteration.
		 *          Yields the wait target for the next loop iteration. Yields None if the loop should finish.
		 *          Receives a pointer that contains true while this process should hurry to complete itself.
		 * @param exc Implicit execution context
		 * @param log Implicit logging implementation
		 * @return A new looping process.
		 */
		def apply(f: Flag => Option[WaitTarget])(implicit exc: ExecutionContext, log: Logger): LoopingProcess = {
			val loop = new _LoopingProcess(startDelayView.getOrElse(View.fixed(NoWait)), waitLock, shutdownReaction,
				isRestartable)(f)
			if (startsImmediately)
				loop.runAsync()
			loop
		}
		
		private def _regularly[U](target: WaitTarget, waitFirst: Boolean = false)(f: Flag => U)
		                         (implicit exc: ExecutionContext, log: Logger) =
		{
			val factory = {
				if (waitFirst)
					after(target)
				else
					this
			}
			factory.continuously { hurryFlag => f(hurryFlag); target }
		}
	}
	
	private class _LoopingProcess(startDelayView: View[WaitTarget], waitLock: AnyRef,
	                              shutdownReaction: ShutdownReaction, override val isRestartable: Boolean)
	                             (f: Flag => Option[WaitTarget])
	                             (implicit exc: ExecutionContext, logger: Logger)
		extends LoopingProcess(startDelayView, waitLock, shutdownReaction)
	{
		override protected def iteration() = f(hurryFlag)
	}
}

/**
  * Loops are operations that can be repeated multiple times.
  * Loops can be broken and sometimes restarted. Care should be taken when sharing process instances, as they have a
  * mutable state.
  * @author Mikko Hilpinen
  * @since 31.3.2019
  * @constructor Creates a new looping process that is not active yet
  * @param startDelayView A view that yields the delay applied before the first iteration (default = no delay)
  * @param waitLock Wait lock to utilize (default = new lock)
  * @param shutdownReaction How this process should react to JVM shutdowns (default = terminate)
  * @param exc Implicit execution context
  * @param logger Logger that records exceptions caught during the scheduled actions
  **/
abstract class LoopingProcess(startDelayView: View[WaitTarget] = View.fixed(NoWait), waitLock: AnyRef = new AnyRef,
                              shutdownReaction: ShutdownReaction = Cancel)
                             (implicit exc: ExecutionContext, logger: Logger)
	extends Process(waitLock, Some(shutdownReaction))
{
	// ABSTRACT    -----------------
	
	/**
	  * The function to perform periodically
	  * @return The next wait target. None if this loop should then break.
	  */
	protected def iteration(): Option[WaitTarget]
	
	
	// COMPUTED --------------------
	
	/**
	  * Converts this loop into a timed task
	  * @return A regularly performable task based on this loop.
	  *         None if this loop was only scheduled to run on call, which is beyond the capabilities of [[TimedTask]]s.
	  *
	  *         Note: If [[iteration]]() ever yields [[WaitTarget.UntilNotified]], this task will no longer be run,
	  *         as TimedTasks don't have this feature.
	  */
	def toTimedTask =
		startDelayView.value.endTime.map { TimedTask.firstTimeAt(_).completing { iteration().flatMap { _.endTime } } }
	
	
	// IMPLEMENTED    --------------
	
	override protected def runOnce(): Unit = {
		// Performs the initial delay, if one has been specified
		var broken = {
			val startDelay = startDelayView.value
			if (startDelay.isPositive) {
				// If the initial wait is interrupted, considers this loop as broken
				val waitCompleted = Wait(startDelay, waitLock)
				if (waitCompleted)
					false
				else {
					markAsInterrupted()
					true
				}
			}
			else
				false
		}
		// Iterates as long as
		// a) New iterations are scheduled
		// b) This process is not broken or requested to hurry
		while (!broken && !shouldHurry) {
			// Performs the operation. Exceptions break this loop.
			iteration() match {
				// Case: New iteration requested => Waits the appropriate period first
				case Some(waitTarget) =>
					if (shouldHurry)
						broken = true
					// Wait interruptions terminate this loop also
					else if (!Wait(waitTarget, waitLock)) {
						markAsInterrupted()
						broken = true
					}
				//  Case: Natural loop completion
				case None => broken = true
			}
		}
	}
	
	
	// OTHER    ---------------------
	
	/**
	  * Skips the current waiting process, if active.
	  * Causes this loop to run the next iteration() function, if this loop was in waiting mode.
	  */
	def skipWait(): Unit = WaitUtils.notify(waitLock)
	
	/**
	  * Starts this loop as a timed task, if possible
	  * @param tasks An implicit [[TimedTasks]] instance for running this loop.
	  *              Note: This method assumes that 'tasks' has been started already,
	  *                    and will not explicitly start this tasks-running process.
	  * @return Whether this loop was successfully converted into a [[TimedTask]] and assigned to 'tasks'.
	  *         False if this loop is based on wait notifications, which are beyond the capabilities of timed tasks.
	  */
	def runAsTimedTask()(implicit tasks: TimedTasks) = toTimedTask match {
		case Some(task) =>
			tasks += task
			true
		case None => false
	}
}
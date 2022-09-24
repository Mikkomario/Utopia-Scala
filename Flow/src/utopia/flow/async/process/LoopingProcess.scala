package utopia.flow.async.process

import utopia.flow.time.WaitTarget.WeeklyTime
import utopia.flow.time.WeekDay
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.ChangingLike

import java.time.LocalTime
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object LoopingProcess
{
	// OTHER    -----------------------------
	
	/**
	  * Creates a new looping process
	  * @param startDelay A delay to perform before the first iteration (default = no delay)
	  * @param waitLock Wait lock to use (optional)
	  * @param shutdownReaction How this loop should react to JVM shutdown (default = stop / cancel everything)
	  * @param isRestartable Whether this loop should be restartable (default = true)
	  * @param f The function that will be called regularly. Accepts a pointer that shows whether the function should
	  *          hurry to complete itself. Returns the next wait target or None if this loop should be broken
	  *          afterwards.
	  * @param exc Implicit execution context
	  * @return A new looping process
	  */
	def apply(startDelay: WaitTarget = WaitTarget.zero, waitLock: AnyRef = new AnyRef,
	          shutdownReaction: ShutdownReaction = Cancel, isRestartable: Boolean = true)
	         (f: => ChangingLike[Boolean] => Option[WaitTarget])
	         (implicit exc: ExecutionContext, logger: Logger): LoopingProcess =
		new FunctionalLoop(startDelay, waitLock, shutdownReaction, isRestartable)(f)
	
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
	def static[U](interval: FiniteDuration, waitLock: AnyRef = new AnyRef, waitFirst: Boolean = false,
	              isRestartable: Boolean = true)
	             (f: => ChangingLike[Boolean] => U)
	             (implicit exc: ExecutionContext, logger: Logger) =
		apply(if (waitFirst) interval else WaitTarget.zero, waitLock, isRestartable = isRestartable) { p =>
			f(p)
			Some(interval)
		}
	
	/**
	  * Creates a new infinite looping process that iterates once every day
	  * @param runTime Time of day when this process should be run
	  * @param isRestartable Whether this loop should be restartable (default = true)
	  * @param f The function that will be called regularly. Accepts a pointer that shows whether the function should
	  *          hurry to complete itself.
	  * @param exc Implicit execution context
	  * @return A new looping process
	  */
	def daily[U](runTime: LocalTime, isRestartable: Boolean = true)
	            (f: => ChangingLike[Boolean] => U)
	            (implicit exc: ExecutionContext, logger: Logger) =
		apply(runTime, isRestartable = isRestartable) { p =>
			f(p)
			Some(runTime)
		}
	
	/**
	  * Creates a new infinite looping process that iterates once a week
	  * @param day Weekday on which the function should be called
	  * @param time Time of day at which the function should be called (on targeted weekday)
	  * @param isRestartable Whether this loop should be restartable (default = true)
	  * @param f A function to all once every week. Accepts a pointer that shows whether the function should
	  *          hurry to complete itself.
	  * @param exc Implicit execution context
	  * @tparam U Arbitrary result type
	  * @return A new process that loops weekly
	  */
	def weekly[U](day: WeekDay, time: LocalTime, isRestartable: Boolean = true)
	             (f: => ChangingLike[Boolean] => U)
	             (implicit exc: ExecutionContext, logger: Logger) =
	{
		val target = WeeklyTime(day, time)
		apply(target, isRestartable = isRestartable) { p =>
			f(p)
			Some(target)
		}
	}
	
	
	// NESTED   -----------------------------
	
	private class FunctionalLoop(startDelay: WaitTarget, waitLock: AnyRef, shutdownReaction: ShutdownReaction,
	                             override val isRestartable: Boolean)
	                            (f: => ChangingLike[Boolean] => Option[WaitTarget])
	                            (implicit exc: ExecutionContext, logger: Logger)
		extends LoopingProcess(startDelay, waitLock, shutdownReaction)
	{
		override protected def iteration() = f(hurryPointer)
	}
}

/**
  * Loops are operations that can be repeated multiple times.
  * Loops can be broken and sometimes restarted. Care should be taken when sharing process instances, as they have a
  * mutable state.
  * @author Mikko Hilpinen
  * @since 31.3.2019
  * @constructor Creates a new looping process that is not active yet
  * @param startDelay Delay applied before the first iteration (default = no delay)
  * @param waitLock Wait lock to utilize (default = new lock)
  * @param shutdownReaction How this process should react to JVM shutdowns (default = terminate)
  * @param exc Implicit execution context
  * @param logger Logger that records exceptions caught during the scheduled actions
  **/
abstract class LoopingProcess(startDelay: WaitTarget = WaitTarget.zero, waitLock: AnyRef = new AnyRef,
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
	
	
	// IMPLEMENTED    --------------
	
	override protected def runOnce() = {
		// Performs the initial delay, if one has been specified
		var broken = {
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
}
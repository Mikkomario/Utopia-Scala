package utopia.flow.async.process

import utopia.flow.collection.mutable.VolatileList
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.WaitTarget.{DailyTime, WeeklyTime}
import utopia.flow.time.{Now, Today, WeekDay}
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.logging.Logger

import java.time.{Instant, LocalTime}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
  * A process which completes a set of timed tasks, which may be repeated
  * @author Mikko Hilpinen
  * @since 24.2.2022, v1.15
  */
class TimedTasks(waitLock: AnyRef = new AnyRef, shutdownReaction: ShutdownReaction = Cancel)
                (implicit exc: ExecutionContext, logger: Logger)
	extends Process(waitLock, Some(shutdownReaction))
{
	// ATTRIBUTES   ----------------------------
	
	private val queue = VolatileList[(Instant, () => Option[Instant])]()
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def isRestartable = true
	
	override protected def runOnce() = {
		// Performs the next task in the ordered loops (unless empty)
		while (!shouldHurry && queue.nonEmpty) {
			// Waits until its time to perform the next task
			val expectedStartTime = queue.head._1
			// If the wait is interrupted, this queue is also considered interrupted
			if (!Wait(expectedStartTime, waitLock))
				markAsInterrupted()
			
			// The tasks may have been altered and a notify may have broken the wait
			if (!shouldHurry) {
				queue.pop().foreach { case (nextTaskTime, nextTask) =>
					// Case: It's time to perform that task
					if (Now >= nextTaskTime) {
						// Performs the task
						// Catches any thrown exceptions and prints them
						Try { nextTask() }.getOrMap { error =>
							logger(error, "TimedTasks operation threw an exception")
							None
						}.foreach { nextTime =>
							// Pushes the task back to the queue
							_addTask(nextTime, nextTask)
						}
					}
					// Case: It wasn't time to perform the task yet
					else
						queue +:= (nextTaskTime -> nextTask)
				}
			}
		}
		// Purges the queue if hurried
		if (shouldHurry)
			queue.clear()
	}
	
	
	// OTHER    -----------------------------
	
	/**
	  * Adds a new task. Restarts this process if it had completed.
	  * @param time Time when this task should be performed
	  * @param task The task that should be performed.
	  *             Returns the next time this task should be performed or None if it shouldn't be repeated.
	  */
	def add(time: Instant)(task: => Option[Instant]) = {
		_addTask(time, () => task)
		// Restarts if necessary (but not if broken)
		if (state == Completed)
			runAsync()
	}
	/**
	  * Adds a new task. Restarts this process if it had completed. The specified task is only run once.
	  * @param time Time when the task should be run
	  * @param task Task to run
	  * @tparam U Arbitrary result type
	  */
	def addOnce[U](time: Instant)(task: => U) = add(time) {
		task
		None
	}
	/**
	  * Adds a new task. Restarts this process if it had completed. The specified task is run between regular intervals.
	  * @param interval Interval between task runs
	  * @param task Task to run
	  * @tparam U Arbitrary result type
	  */
	def addLoop[U](interval: FiniteDuration)(task: => U) = add(Now + interval) {
		task
		Some(Now + interval)
	}
	/**
	  * Adds a new task. Restarts this process if it had completed.
	  * The specified task is run every day at the specified time.
	  * @param time Time when this task should be performed
	  * @param task Task to run
	  * @tparam U Arbitrary result type
	  */
	def addDaily[U](time: LocalTime)(task: => U) = {
		DailyTime(time).endTime.foreach { firstTime =>
			add(firstTime) {
				task
				Some(Today.tomorrow.atTime(time).toInstantInDefaultZone)
			}
		}
	}
	/**
	  * Adds a new task. Restarts this process if it had completed. The specified task is run every week at the
	  * specified weekday + time.
	  * @param weekday Day of week on which the task should be run
	  * @param time Time of day at which the task should be run on that week day
	  * @param task The task to run
	  * @tparam U Arbitrary result type
	  */
	def addWeekly[U](weekday: WeekDay, time: LocalTime)(task: => U) = {
		WeeklyTime(weekday, time).endTime.foreach { firstTime =>
			add(firstTime) {
				task
				Some(Today.next(weekday).atTime(time).toInstantInDefaultZone)
			}
		}
	}
	
	private def _addTask(taskTime: Instant, task: () => Option[Instant]) = {
		// Adds the item to the correct position in the queue
		val isFirst = queue.pop { q =>
			val tasksBefore = q.takeWhile { _._1 <= taskTime }
			val tasksAfter = q.drop(tasksBefore.size)
			tasksBefore.isEmpty -> ((tasksBefore :+ (taskTime -> task)) ++ tasksAfter)
		}
		// If the new item is the first item, reschedules the main loop
		if (isFirst)
			WaitUtils.notify(waitLock)
	}
}

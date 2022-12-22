package utopia.flow.async.process

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.ProcessState.Completed
import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.time.TimeExtensions._
import utopia.flow.async.process.WaitTarget.{DailyTime, WeeklyTime}
import utopia.flow.time.{Now, Today, WeekDay}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.Logger

import java.time.{Instant, LocalTime}
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

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
	
	// First item is the next task run time
	// Second item is the actual operation, which may either may be asynchronous (Right)
	private val queue = VolatileList[(Instant, () => Either[Option[Instant], Future[Option[Instant]]])]()
	
	
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
						// Performs or starts the task
						val nextTime = Try { nextTask() }
							// Catches errors during running / starting
							.getOrMap { error =>
								logger(error, "TimedTasks operation threw an exception")
								Left(None)
							}
							// If this queue would become empty, won't perform the tasks asynchronously (blocks here)
							// This is to avoid situations where this queue would accidentally become empty and
							// terminate
							.divergeMapRight { future =>
								// Case: Performing last task asynchronously => Blocks until completed
								if (queue.isEmpty)
									future.waitFor() match {
										case Success(nextTime) => Left(nextTime)
										case Failure(error) =>
											logger(error, "Timed task threw an exception")
											Left(None)
									}
								// Case: There were other tasks => Continues asynchronous performance
								else
									Right(future)
							}
						// Pushes the item back into the queue when / if the next run time is known
						nextTime match {
							// Case: Time is immediately known (task blocked)
							case Left(nextTime) => nextTime.foreach { nextTime => _addTask(nextTime, nextTask) }
							// Case: Time is known later (async task)
							case Right(nextTimeFuture) =>
								nextTimeFuture.onComplete {
									case Success(nextTime) =>
										nextTime.foreach { nextTime => _addTask(nextTime, nextTask) }
									case Failure(error) => logger(error, "Asynchronous TimedTask threw an exception")
								}
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
		_addTask(time, () => Left(task))
		// Restarts if necessary (but not if broken)
		if (state == Completed)
			runAsync()
	}
	
	/**
	  * Adds a new timed task. Restarts this process if it had completed already.
	  * Note: Doesn't start this process if this process has not yet started.
	  * @param task A task to add to the performed tasks
	  */
	def +=(task: TimedTask): Unit = {
		_addTask(task.firstRunTime, () => task.run())
		// Restarts if necessary (but not if broken)
		if (state == Completed)
			runAsync()
	}
	/**
	  * Adds new timed tasks. Restarts this process if it had completed already.
	  * Note: Doesn't start this process if this process has not yet started.
	  * @param tasks Tasks to add to the performed tasks
	  */
	def ++=(tasks: IterableOnce[TimedTask]): Unit = {
		val iter = tasks.iterator
		if (iter.hasNext) {
			iter.foreach { t => _addTask(t.firstRunTime, () => t.run()) }
			// Restarts if necessary (but not if broken)
			if (state == Completed)
				runAsync()
		}
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
	
	private def _addTask(taskTime: Instant, task: () => Either[Option[Instant], Future[Option[Instant]]]) = {
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

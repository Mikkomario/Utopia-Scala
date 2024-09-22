package utopia.flow.async.process

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.ProcessState.Completed
import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.async.process.WaitTarget.{DailyTime, UntilNotified, WeeklyTime}
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse
import utopia.flow.operator.MaybeEmpty
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Now, Today, WeekDay}
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.template.eventful.Changing

import java.time.{Instant, LocalTime}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

/**
  * A process which completes a set of timed tasks, which may be repeated
  * @author Mikko Hilpinen
  * @since 24.2.2022, v1.15
  */
class TimedTasks(waitLock: AnyRef = new AnyRef, shutdownReaction: ShutdownReaction = Cancel)
                (implicit exc: ExecutionContext, logger: Logger)
	extends Process(waitLock, Some(shutdownReaction)) with MaybeEmpty[TimedTasks]
{
	// ATTRIBUTES   ----------------------------
	
	// First item is the next task run time
	// Second item is the actual operation, which may either may be asynchronous (Right)
	private val queue = Volatile.seq[(Changing[Option[Instant]], () => Future[Changing[Option[Instant]]])]()
	private val nextWaitTarget = Volatile[WaitTarget](UntilNotified)
	
	private val scheduleTimeChangeListener = ChangeListener.onAnyChange {
		val newWaitTarget = queue.mutate { queue =>
			val sortedQueue = queue.sortBy { _._1.value }
			// Checks what's the next wait time
			sortedQueue.findMap { _._1.value } -> sortedQueue
		}
		if (nextWaitTarget.value.endTime != newWaitTarget)
			WaitUtils.notify(waitLock)
		// Changes in scheduled task times trigger restarts
		if (state == Completed)
			runAsync()
		
		ChangeResponse.continueIf(state.isNotBroken)
	}
	
	
	// IMPLEMENTED  ----------------------------
	
	override def self: TimedTasks = this
	
	override protected def isRestartable = true
	
	// This set of tasks is considered empty if none of the scheduled tasks specify a run time
	override def isEmpty: Boolean = queue.value.forall { _._1.value.isEmpty }
	
	override protected def runOnce() = {
		// Performs the next task in the ordered loops (unless empty)
		while (!shouldHurry && nonEmpty) {
			// Waits until its time to perform the next task
			val expectedStartTime = queue.value.findMap { _._1.value }
			expectedStartTime.foreach { nextWaitTarget.value = _ }
			// If the wait is interrupted, this queue is also considered interrupted
			if (expectedStartTime.exists { !Wait(_, waitLock) })
				markAsInterrupted()
			
			// The tasks may have been altered and a notify may have broken the wait
			if (!shouldHurry) {
				val nextTaskData = queue.mutate { queue =>
					// Finds the next task that wants to be run (i.e. next run time is defined)
					val nextTaskData = queue.iterator.zipWithIndex
						.findMap { case ((timePointer, task), index) =>
							timePointer.value.map { time => (time, timePointer, task, index) }
						}
					nextTaskData match {
						// Case: Found the task
						case Some((nextTaskTime, timePointer, task, index)) =>
							// Case: It is not yet time to perform the task => Keeps it in the queue
							if (nextTaskTime > Now)
								None -> queue
							// Case: It is time to perform the task => Removes it from the queue
							else
								Some((timePointer, task)) -> queue.withoutIndex(index)
						// Case: Didn't find a task that wanted to be run => Keeps the queue as is
						case None => None -> queue
					}
				}
				nextTaskData.foreach { case (timePointer, nextTask) =>
					timePointer.removeListener(scheduleTimeChangeListener)
					// Performs or starts the task
					val taskCompletion = Try { nextTask() }
						// Catches errors during running / starting
						.getOrMap { error =>
							logger(error, "TimedTasks operation threw an exception")
							Future.successful(Fixed(None))
						}
					// If this queue would become empty, won't perform the tasks asynchronously (blocks here)
					// This is to avoid situations where this queue would accidentally become empty and
					// terminate
					if (isEmpty)
						taskCompletion.waitFor()
					// Pushes the item back into the queue when / if the next run time is known
					taskCompletion.current match {
						// Case: Time is immediately known (task blocked)
						case Some(newTimePointer) => _addTask(newTimePointer, nextTask)
						// Case: Time is known later (async task)
						case None =>
							taskCompletion.onComplete {
								case Success(newTimePointer) => _addTask(newTimePointer, nextTask)
								case Failure(error) => logger(error, "Task failed to complete")
							}
					}
				}
			}
		}
		// Purges the queue if hurried
		if (shouldHurry || state.isBroken)
			queue.popAll().foreach { _._1.removeListener(scheduleTimeChangeListener) }
	}
	
	
	// OTHER    -----------------------------
	
	def addCancellableAsync(firstTimePointer: Changing[Option[Instant]])
	                       (task: => Future[Changing[Option[Instant]]]) =
	{
		_addTask(firstTimePointer, () => task)
		// Restarts if necessary (but not if broken)
		restartIfCompleted()
	}
	def addCancellable(firstTimePointer: Changing[Option[Instant]])
	               (task: => Changing[Option[Instant]]) =
		addCancellableAsync(firstTimePointer) { Future.successful(task) }
	// Wraps the scheduled times into "Some", meaning that tasks won't ever be cancelled
	def addChangingAsync(firstTimePointer: Changing[Instant])
	                    (task: => Future[Changing[Instant]]) =
		addCancellableAsync(firstTimePointer.lightMap { Some(_) }) { task.map { _.lightMap { Some(_) } } }
	def addChanging(firstTimePointer: Changing[Instant])(task: => Changing[Instant]) =
		addCancellable(firstTimePointer.lightMap { Some(_) }) { task.lightMap { Some(_) } }
	def addCompletingAsync(firstTime: Instant)(task: => Future[Option[Instant]]) =
		addCancellableAsync(Fixed(Some(firstTime))) { task.map { Fixed(_) } }
	def addCompleting(firstTime: Instant)(task: => Option[Instant]) =
		addCancellable(Fixed(Some(firstTime))) { Fixed(task) }
	def add(time: Instant)(task: => Instant) = addCompleting(time) { Some(task) }
	
	/**
	  * Adds a new timed task. Restarts this process if it had completed already.
	  * Note: Doesn't start this process if this process has not yet started.
	  * @param task A task to add to the performed tasks
	  */
	def +=(task: TimedTask): Unit =
		addCancellableAsync(Fixed(Some(task.firstRunTime))) { task.run() }
	/**
	  * Adds new timed tasks. Restarts this process if it had completed already.
	  * Note: Doesn't start this process if this process has not yet started.
	  * @param tasks Tasks to add to the performed tasks
	  */
	def ++=(tasks: IterableOnce[TimedTask]): Unit = {
		val iter = tasks.iterator
		if (iter.hasNext) {
			iter.foreach { t => _addTask(Fixed(Some(t.firstRunTime)), () => t.run()) }
			// Restarts if necessary (but not if broken)
			restartIfCompleted()
		}
	}
	
	/**
	  * Adds a new task. Restarts this process if it had completed. The specified task is only run once.
	  * @param time Time when the task should be run
	  * @param task Task to run
	  * @tparam U Arbitrary result type
	  */
	def addOnce[U](time: Instant)(task: => U) = addCompleting(time) {
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
		Now + interval
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
				Today.tomorrow.atTime(time).toInstantInDefaultZone
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
				Today.next(weekday).atTime(time).toInstantInDefaultZone
			}
		}
	}
	
	private def _addTask(taskTimePointer: Changing[Option[Instant]],
	                     task: () => Future[Changing[Option[Instant]]]) =
	{
		// Adds the item to the correct position in the queue
		if (!taskTimePointer.existsFixed { _.isEmpty }) {
			taskTimePointer.addListener(scheduleTimeChangeListener)
			val knownTaskTime = taskTimePointer.value
			val waitTimeChanged = queue.mutate { q =>
				val filteredQueue = q.filterNot { _._1.existsFixed { _.isEmpty } }
				knownTaskTime match {
					// Case: Next task run-time is estimated => Places the task at the right place in the queue
					case Some(knownTaskTime) =>
						val tasksBefore = filteredQueue.takeWhile { _._1.value.forall { _ <= knownTaskTime } }
						val tasksAfter = filteredQueue.drop(tasksBefore.size)
						// Checks whether this affects the next task wait time
						tasksBefore.forall { _._1.value.isEmpty } ->
							((tasksBefore :+ (taskTimePointer -> task)) ++ tasksAfter)
					// Case: Next task run-time is not given => Places the task at the beginning of the queue
					// (but ignores it)
					case None =>
						false -> ((taskTimePointer -> task) +: filteredQueue)
				}
			}
			// If the new item is the first item, reschedules the main loop
			if (waitTimeChanged)
				WaitUtils.notify(waitLock)
		}
	}
	
	private def restartIfCompleted() = {
		if (state == Completed)
			runAsync()
	}
}

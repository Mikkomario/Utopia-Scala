package utopia.flow.async.process

import utopia.flow.async.process.WaitTarget.Until
import utopia.flow.operator.CombinedOrdering
import utopia.flow.time.{Now, Today, WeekDay}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger

import java.time.{Instant, LocalTime}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object TimedTask
{
	// OTHER    ----------------------------
	
	/**
	  * Creates a new timed task
	  * @param firstTime The first time this task should be run (call-by-name)
	  * @param f A function that performs the task and returns the next time this task should be run,
	  *          or None if this task shouldn't be run anymore.
	  * @return A new timed task
	  */
	def apply(firstTime: => Instant)(f: => Option[Instant]): TimedTask = new _TimedTask(firstTime)(f)
	
	/**
	  * Creates a new timed task that runs only once
	  * @param time The time when the task should be run (call-by-name)
	  * @param f A function that performs the task
	  * @tparam U Arbitrary function result type
	  * @return A new timed task
	  */
	def once[U](time: => Instant)(f: => U) = apply(time) {
		f
		None
	}
	
	/**
	  * Creates a new timed task that is run at regular intervals
	  * @param interval Interval between task runs
	  * @param startImmediately Whether the task should be run ASAP, initially (default = false)
	  * @param f Task to run
	  * @tparam U Arbitrary function result type
	  * @return A new timed task
	  */
	def regularly[U](interval: FiniteDuration, startImmediately: Boolean = false)(f: => U) =
		apply { if (startImmediately) Now.toInstant else Now + interval } {
			val start = Now.toInstant
			f
			Some(start + interval)
		}
	/**
	  * Creates a new timed task that is run at regular intervals, as long as a condition is met
	  * @param interval Interval between task runs
	  * @param startImmediately Whether the task should be run ASAP, initially (default = false)
	  * @param f Task to run. Returns whether the task should be run again later.
	  * @return A new timed task
	  */
	def regularlyWhile(interval: FiniteDuration, startImmediately: Boolean = false)(f: => Boolean) =
		apply { if (startImmediately) Now.toInstant else Now + interval } {
			val start = Now.toInstant
			if (f) Some(start + interval) else None
		}
	
	/**
	  * Creates a new timed task that is run once every day at a specific time
	  * @param time Time when this task should be run (local)
	  * @param f Task to run
	  * @tparam U Arbitrary function result type
	  * @return A new timed task
	  */
	def dailyAt[U](time: LocalTime)(f: => U) =
		apply {
			val today = Today.atTime(time)
			(if (today >= Now) today else today + 1.days).toInstantInDefaultZone
		} {
			f
			Some(Today.tomorrow.atTime(time).toInstantInDefaultZone)
		}
	/**
	  * Creates a new timed task that is run at specific times every day
	  * @param time Time when this task should be run (local)
	  * @param secondTime Another time when this task should be run (local)
	  * @param moreTimes More times when this task should be run (local)
	  * @param f Task to run
	  * @tparam U Arbitrary function result type
	  * @return A new timed task
	  */
	def dailyAt[U](time: LocalTime, secondTime: LocalTime, moreTimes: LocalTime*)(f: => U) = {
		val times = (Vector(time, secondTime) ++ moreTimes).sorted
		def nextTime = {
			val currentTime = Now.toLocalTime
			(times.find { _ >= currentTime } match {
				case Some(time) => Today + time
				case None => Today.tomorrow + times.head
			}).toInstantInDefaultZone
		}
		apply { nextTime } {
			f
			Some(nextTime)
		}
	}
	
	/**
	  * Creates a new timed task that is run once a week at a specific time
	  * @param day The week day when this task should be run
	  * @param time The time of day when this task should be run
	  * @param f Task to run
	  * @tparam U Arbitrary function result type
	  * @return A new timed task
	  */
	def weeklyAt[U](day: WeekDay, time: LocalTime)(f: => U) = {
		apply {
			val today = Today.weekDay
			val daysWait = today.until(day)
			val timeWait = time - Now.toLocalTime
			Now + daysWait + timeWait
		} {
			val start = Now.toInstant
			f
			Some(start + 7.days)
		}
	}
	/**
	  * Creates a new timed task that is run at specific times every week
	  * @param times Times when this task should be run.
	  *              Consists of two parts:
	  *              1: The week day on which this task should be run and
	  *              2: The (local) time of day when this task should be run
	  * @param f The task to run
	  * @tparam U U Arbitrary function result type
	  * @return new timed task
	  */
	def weeklyAt[U](times: IterableOnce[(WeekDay, LocalTime)])(f: => U) = {
		val _times = Vector.from(times).sorted(CombinedOrdering(
			Ordering.by { p: (WeekDay, LocalTime) => p._1 }, Ordering.by { p: (WeekDay, LocalTime) => p._2 }))
		if (_times.isEmpty)
			apply(Now) { None }
		else {
			def nextTime = {
				val today = Today.weekDay
				val currentTime = Now.toLocalTime
				val (nextDay, nextTime) = _times.find { case (day, time) =>
					today >= day && currentTime >= time }
					.getOrElse(_times.head)
				val daysWait = today.until(nextDay)
				val timeWait = nextTime - currentTime
				Now + daysWait + timeWait
			}
			apply(nextTime) {
				f
				Some(nextTime)
			}
		}
	}
	/**
	  * Creates a new timed task that is run at specific times every week.
	  * Each time parameter consists of two parts:
	  * 1: The week day on which this task should be run and
	  * 2: The (local) time of day when this task should be run
	  * @param firstTime First task run time
	  * @param secondTime Second task run time
	  * @param more More task run times
	  * @param f The task to run
	  * @tparam U U Arbitrary function result type
	  * @return new timed task
	  */
	def weeklyAt[U](firstTime: (WeekDay, LocalTime), secondTime: (WeekDay, LocalTime), more: (WeekDay, LocalTime)*)
	               (f: => U): TimedTask =
		weeklyAt[U](Vector(firstTime, secondTime) ++ more)(f)
	
	
	// NESTED   ----------------------------
	
	private class _TimedTask(firstTime: => Instant)(f: => Option[Instant]) extends TimedTask {
		override def firstRunTime = firstTime
		override def run() = f
	}
}

/**
  * A common trait for simple looping functions that are run at specific times
  * @author Mikko Hilpinen
  * @since 10.10.2022, v2.0
  */
trait TimedTask
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The first time when this task should be (or should have been) run.
	  */
	def firstRunTime: Instant
	
	/**
	  * Runs this task once
	  * @return The next time when this task should be run. None if this task shouldn't be run anymore.
	  */
	def run(): Option[Instant]
	
	
	// COMPUTED ---------------------------
	
	/**
	  * Converts this timed task to a loop.
	  * Note: TimedTask instances are often used with a TimedTasks instance and not as separate loops.
	  * @param exc Implicit execution context to use
	  * @param logger Implicit logger that receives thrown errors
	  * @return A new loop based on this task
	  */
	def toLoop(implicit exc: ExecutionContext, logger: Logger) =
		LoopingProcess(Until(firstRunTime)) { _ => run().map { Until(_) } }
	
	
	// OTHER    ---------------------------
	
	/**
	  * Converts this timed task to a loop and starts it.
	  * Note: TimedTask instances are often used with a TimedTasks instance and not as separate loops.
	  * @param exc Implicit execution context to use
	  * @param logger Implicit logger that receives thrown errors
	  * @return A new, already running, loop based on this task
	  */
	def loop()(implicit exc: ExecutionContext, logger: Logger) = {
		val l = toLoop
		l.runAsync()
		l
	}
}
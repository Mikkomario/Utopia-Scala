package utopia.flow.async.process

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.process.ShutdownReaction.Cancel
import utopia.flow.async.process.WaitTarget.{Until, UntilNotified}
import utopia.flow.event.model.ChangeResponse.{Continue, Detach}
import utopia.flow.event.model.ChangeResult
import utopia.flow.operator.ordering.CombinedOrdering
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Now, Today, WeekDay, WeekDays}
import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Changing

import java.time.{Instant, LocalTime}
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object TimedTask
{
	// ATTRIBUTE    ------------------------
	
	/**
	  * A factory that constructs timed tasks that are started immediately
	  */
	val immediately = firstTimeAt(Now)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param time The first time the created task(s) should be run (call-by-name)
	  * @return A factory used for constructing timed tasks
	  */
	def firstTimeAt(time: => Instant) = new TimedTaskFactory(time)
	
	/**
	  * Creates a new timed task
	  * @param firstTime The first time this task should be run (call-by-name)
	  * @param f A function that performs the task and returns the next time this task should be run,
	  *          or None if this task shouldn't be run anymore.
	  * @return A new timed task
	  */
	@deprecated("Please use .firstTimeAt(Instant).completing(...) instead", "v2.3")
	def apply(firstTime: => Instant = Now)(f: => Option[Instant]): TimedTask = firstTimeAt(firstTime).completing(f)
	/**
	  * Creates a new timed task
	  * @param firstTime The first time this task should be run (call-by-name)
	  * @param f         A function that starts the task and returns a future
	  *                  that resolves to the next time this task should be run,
	  *                  or None if this task shouldn't be run anymore.
	  * @return A new timed task
	  */
	@deprecated("Please use .firstTimeAt(Instant).completingAsync(...) instead", "v2.3")
	def async(firstTime: => Instant = Now)(f: => Future[Option[Instant]])
	         (implicit executionContext: ExecutionContext): TimedTask =
		firstTimeAt(firstTime).completingAsync(f)
	
	/**
	  * Creates a new timed task that runs only once
	  * @param time The time when the task should be run (call-by-name)
	  * @param f A function that performs the task
	  * @tparam U Arbitrary function result type
	  * @return A new timed task
	  */
	@deprecated("Please use .firstTimeAt(Instant).once(...) instead", "v2.3")
	def once[U](time: => Instant)(f: => U) = firstTimeAt(time).once(f)
	
	/**
	  * Creates a new timed task that is run at regular intervals
	  * @param interval Interval between task runs
	  * @param startImmediately Whether the task should be run ASAP, initially (default = false)
	  * @param f Task to run
	  * @tparam U Arbitrary function result type
	  * @return A new timed task
	  */
	def regularly[U](interval: FiniteDuration, startImmediately: Boolean = false)(f: => U) =
		firstTimeAt(if (startImmediately) Now else Now + interval) {
			val start = Now.toInstant
			f
			start + interval
		}
	/**
	  * Creates a new timed task that is run at regular intervals, as long as a condition is met
	  * @param interval Interval between task runs
	  * @param startImmediately Whether the task should be run ASAP, initially (default = false)
	  * @param f Task to run. Returns whether the task should be run again later.
	  * @return A new timed task
	  */
	def regularlyWhile(interval: FiniteDuration, startImmediately: Boolean = false)(f: => Boolean) =
		firstTimeAt(if (startImmediately) Now else Now + interval).completing {
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
		firstTimeAt {
			val today = Today.atTime(time)
			(if (today >= Now) today else today + 1.days).toInstantInDefaultZone
		} {
			f
			Today.tomorrow.atTime(time).toInstantInDefaultZone
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
		firstTimeAt { nextTime } { f; nextTime }
	}
	
	/**
	  * Creates a new timed task that is run once a week at a specific time
	  * @param day The week day when this task should be run
	  * @param time The time of day when this task should be run
	  * @param f Task to run
	  * @param w Week days calendar system
	  * @tparam U Arbitrary function result type
	  * @return A new timed task
	  */
	def weeklyAt[U](day: WeekDay, time: LocalTime)(f: => U)(implicit w: WeekDays) = {
		firstTimeAt {
			val today = Today.weekDay
			val daysWait = today.until(day)
			val timeWait = time - Now.toLocalTime
			Now + daysWait + timeWait
		} {
			val start = Now.toInstant
			f
			start + 7.days
		}
	}
	/**
	  * Creates a new timed task that is run at specific times every week
	  * @param times Times when this task should be run.
	  *              Consists of two parts:
	  *              1: The week day on which this task should be run and
	  *              2: The (local) time of day when this task should be run
	  * @param f The task to run
	  * @param w Week days calendar system
	  * @tparam U U Arbitrary function result type
	  * @return new timed task
	  */
	def weeklyAt[U](times: IterableOnce[(WeekDay, LocalTime)])(f: => U)(implicit w: WeekDays) = {
		val _times = Vector.from(times).sorted(CombinedOrdering(
			Ordering.by { p: (WeekDay, LocalTime) => p._1 }, Ordering.by { p: (WeekDay, LocalTime) => p._2 }))
		if (_times.isEmpty)
			immediately.once(())
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
			firstTimeAt (nextTime) { f; nextTime }
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
	  * @param w Week days calendar system
	  * @param f The task to run
	  * @tparam U U Arbitrary function result type
	  * @return new timed task
	  */
	def weeklyAt[U](firstTime: (WeekDay, LocalTime), secondTime: (WeekDay, LocalTime), more: (WeekDay, LocalTime)*)
	               (f: => U)(implicit w: WeekDays): TimedTask =
		weeklyAt[U](Vector(firstTime, secondTime) ++ more)(f)
	
	
	// NESTED   ----------------------------
	
	class TimedTaskFactory(firstTime: => Instant)
	{
		/**
		  * @param f A function that resolves into a future that resolves into a pointer
		  *          that determines the next time this process should be run.
		  *          The pointer will contain None if the run should be (temporarily) cancelled.
		  * @return A new timed task that calls the specified function and uses a
		  *         pointer-based rescheduling logic
		  */
		def cancellableAsync(f: => Future[Changing[Option[Instant]]]): TimedTask =
			new _TimedTask(firstTime)(f)
		/**
		  * @param f A function that resolves into a pointer
		  *          that determines the next time this process should be run.
		  *          The pointer will contain None if the run should be (temporarily) cancelled.
		  * @return A new timed task that calls the specified function and uses a
		  *         pointer-based rescheduling logic
		  */
		def cancellable(f: => Changing[Option[Instant]]) = cancellableAsync { Future.successful(f) }
		/**
		  * @param f A function that resolves into a future that resolves into a pointer
		  *          that determines the next time this process should be run.
		  * @return A new timed task that calls the specified function and uses a
		  *         pointer-based rescheduling logic
		  */
		def changingAsync(f: => Future[Changing[Instant]])(implicit exc: ExecutionContext) =
			cancellableAsync { f.map { _.lightMap { Some(_) } } }
		/**
		  * @param f A function that resolves into a pointer
		  *          that determines the next time this process should be run.
		  * @return A new timed task that calls the specified function and uses a
		  *         pointer-based rescheduling logic
		  */
		def changing(f: => Changing[Instant]) = cancellable { f.lightMap { Some(_) } }
		/**
		  * @param f A function that resolves into a future that resolves into the
		  *          next time this process should be run, or to None, in case this process should be stopped.
		  * @return A new timed task that calls the specified function
		  */
		def completingAsync(f: => Future[Option[Instant]])(implicit exc: ExecutionContext) =
			cancellableAsync { f.map { Fixed(_) } }
		/**
		  * @param f A function that resolves into the
		  *          next time this process should be run, or to None, in case this process should be stopped.
		  * @return A new timed task that calls the specified function
		  */
		def completing(f: => Option[Instant]) = cancellable { Fixed(f) }
		/**
		  * @param f A function that resolves into a future that resolves into the
		  *          next time this process should be run.
		  * @return A new timed task that calls the specified function
		  */
		def async(f: => Future[Instant])(implicit exc: ExecutionContext) =
			cancellableAsync { f.map { t => Fixed(Some(t)) } }
		/**
		  * @param f A function that resolves into the next time this process should be run
		  * @return A new timed task that calls the specified function
		  */
		def apply(f: => Instant) = cancellable { Fixed(Some(f)) }
		/**
		  * @param f Function that should be run once
		  * @tparam U Arbitrary function result type
		  * @return A new timed task that runs only once
		  */
		def once[U](f: => U) = completing { f; None }
	}
	
	private class _TimedTask(firstTime: => Instant)(f: => Future[Changing[Option[Instant]]])
		extends TimedTask
	{
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
	  * @return Future that resolves into a pointer that determines the next time this task shall be run.
	  *         The pointer will contain None in case this task should not be run anymore.
	  */
	def run(): Future[Changing[Option[Instant]]]
	
	
	// COMPUTED ---------------------------
	
	/**
	  * Converts this timed task to a loop.
	  * Note: TimedTask instances are often used with a TimedTasks instance and not as separate loops.
	  * @param exc Implicit execution context to use
	  * @param logger Implicit logger that receives thrown errors
	  * @return A new loop based on this task
	  */
	def toLoop(implicit exc: ExecutionContext, logger: Logger) = {
		// This pointer contains the next wait target pointer
		val waitPointerPointer = EventfulPointer[Changing[Option[Instant]]](Fixed(Some(firstRunTime)))
		// This pointer converts that pointer value into an applicable wait target
		val waitTargetPointer = waitPointerPointer.flatMap[WaitTarget] { _.map {
			case Some(nextTime) => Until(nextTime)
			case None => UntilNotified
		} }
		// Creates a process to facilitate the running of this task
		val process = PostponingProcess(waitTargetPointer, shutdownReaction = Some(Cancel)) { hurryPointer =>
			val nextTimeFuture = run()
			nextTimeFuture.current match {
				// Case: Blocked
				case Some(nextTimePointer) => waitPointerPointer.value = nextTimePointer
				// Case: Async
				case None =>
					// If has to hurry at any point, terminates the wait
					val lock = new AnyRef
					hurryPointer.onNextChange { _ => WaitUtils.notify(lock) }
					nextTimeFuture.waitWith(lock) match {
						// Case: Success => Schedules next loop, if necessary
						case Success(nextTimePointer) => waitPointerPointer.value = nextTimePointer
						// Case: Failure => Terminates
						case Failure(error) =>
							logger(error)
							waitPointerPointer.value = Fixed(None)
					}
			}
		}
		// If the latest wait target pointer gets fixed to None,
		// cancels / stops this process
		waitPointerPointer.flatMap { _.withState }
			.addListenerAndSimulateEvent(ChangeResult.temporal(None)) { event =>
				if (event.newValue.containsFinal(None))
					Detach.and { process.stop() }
				else
					Continue
			}
		process
	}
	
	
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

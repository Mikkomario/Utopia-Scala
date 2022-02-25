package utopia.flow.async

import java.time.{LocalDateTime, LocalTime}
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.WeekDay
import utopia.flow.time.WaitTarget.Until
import utopia.flow.util.RichComparable._

@deprecated("Replaced with LoopingProcess", "v1.15")
object WeeklyTask
{
	// OTHER	--------------------------
	
	/**
	  * @param weekDay Week day when this task should be run
	  * @param time Time at which this task should be run
	  * @param task Task that will be run
	  * @return A loop that runs the task once a week at specified time
	  */
	@deprecated("Replaced with LoopingProcess.weekly(...)(...)", "v1.15")
	def apply(weekDay: WeekDay, time: LocalTime)(task: => Unit): WeeklyTask = new TaskWrapper(weekDay, time, task)
	
	
	// NESTED	--------------------------
	
	private class TaskWrapper(override val weekDay: WeekDay, override val triggerTime: LocalTime, task: => Unit)
		extends WeeklyTask
	{
		override def runOnce() = task
	}
}

/**
  * Common trait for processes that should be repeated once every week
  * @author Mikko Hilpinen
  * @since 30.7.2020, v1.8
  */
@deprecated("Replaced with LoopingProcess", "v1.15")
trait WeeklyTask extends Loop
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return The week day on which this task should be performed
	  */
	def weekDay: WeekDay
	
	/**
	  * @return The time of day at which this task should be performed
	  */
	def triggerTime: LocalTime
	
	
	// IMPLEMENTED	----------------------
	
	override def nextWaitTarget =
	{
		val now = LocalDateTime.now()
		val today = now.toLocalDate
		val currentWeekDay = today.weekDay
		val currentTime = now.toLocalTime
		
		// Case: Task is scheduled for later today
		if (currentWeekDay == weekDay && currentTime < triggerTime)
			Until((today + triggerTime).toInstantInDefaultZone)
		// Case: Task is scheduled for a later day
		else
			Until((today.next(weekDay) + triggerTime).toInstantInDefaultZone)
	}
}

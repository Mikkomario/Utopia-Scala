package utopia.flow.async

import java.time.{LocalDate, LocalDateTime, LocalTime}

import utopia.flow.util.WaitTarget.Until
import utopia.flow.util.TimeExtensions._

object DailyTask
{
	// OTHER	---------------------
	
	/**
	  * @param triggerTime Time when this task is performed
	  * @param f Function that will be run
	  * @return A new daily task
	  */
	def apply(triggerTime: LocalTime)(f: => Unit): DailyTask = new DailyTaskWrapper(triggerTime)(f)
	
	
	// NESTED	---------------------
	
	private class DailyTaskWrapper(override val triggerTime: LocalTime)(f: => Unit) extends DailyTask
	{
		override def runOnce() = f
	}
}

/**
  * Common trait for processes that should be repeated once every day
  * @author Mikko Hilpinen
  * @since 3.4.2020, v1.7
  */
trait DailyTask extends Loop
{
	// ABSTRACT	----------------------
	
	/**
	  * @return The hour + minute time when this task should be performed
	  */
	def triggerTime: LocalTime
	
	
	// IMPLEMENTED	------------------
	
	override def nextWaitTarget =
	{
		val today = LocalDate.now()
		val timeToday = today + triggerTime
		val now = LocalDateTime.now()
		val waitTargetTime =
		{
			if (now > timeToday)
				timeToday + 1.days
			else
				timeToday
		}
		
		Until(waitTargetTime.toInstantInDefaultZone)
	}
}

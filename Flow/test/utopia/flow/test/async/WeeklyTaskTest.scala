package utopia.flow.test.async

import utopia.flow.async.WeeklyTask
import utopia.flow.generic.DataType
import utopia.flow.time.WeekDay
import utopia.flow.time.TimeExtensions._

import java.time.{Instant, LocalTime}

/**
 * Tests weekly task scheduling
 * @author Mikko Hilpinen
 * @since 29.9.2020, v1.9
 */
@deprecated("Replaced with Loop & LoopingProcess and their tests", "v1.15")
object WeeklyTaskTest extends App
{
	DataType.setup()
	
	val currentDay = WeekDay.current()
	val currentTime = LocalTime.now()
	val startTime = Instant.now()
	
	val loop1 = WeeklyTask(currentDay, currentTime + 0.1.seconds) {}
	assert(loop1.nextWaitTarget.toDuration <= 0.1.seconds)
	
	val loop2 = WeeklyTask(currentDay, currentTime - 0.1.seconds) {}
	val loop2Wait = loop2.nextWaitTarget.toDuration
	println(loop2Wait.description)
	assert(loop2Wait <= 1.weeks)
	assert(loop2Wait >= 6.days + 23.hours)
	
	println("Done")
}

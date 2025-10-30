package utopia.flow.test.async

import utopia.flow.async.process.ProcessState.Running
import utopia.flow.async.process.{TimedTasks, Wait}
import utopia.flow.test.TestContext._
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.Now

/**
 * Tests TimedTasks when adding daily tasks
 * @author Mikko Hilpinen
 * @since 30.10.2025, v2.7
 */
object DailyTasksTest extends App
{
	private val tasks = new TimedTasks(clearTasksOnStop = true)
	private val startTime = Now.toLocalTime
	
	private var counter = 0
	
	tasks.addDaily(startTime + 3.seconds) { counter += 1 }
	tasks.addDaily(startTime + 5.seconds) { counter += 1 }
	tasks.addDaily(startTime + 7.seconds) { counter += 1 }
	
	println("Waiting ~8 seconds for the scheduled tasks to run")
	tasks.runAsync()
	assert(counter == 0)
	Wait(2.seconds)
	assert(counter == 0)
	Wait(2.seconds)
	assert(counter == 1)
	Wait(2.seconds)
	assert(counter == 2)
	Wait(2.seconds)
	assert(counter == 3)
	
	assert(tasks.state == Running)
	
	println("Success (first iteration only)")
}

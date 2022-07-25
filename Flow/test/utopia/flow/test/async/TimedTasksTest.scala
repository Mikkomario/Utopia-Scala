package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.ProcessState.{Completed, NotStarted, Running, Stopped}
import utopia.flow.async.{ThreadPool, TimedTasks, Volatile, Wait}
import utopia.flow.generic.DataType
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * A test for TimedTasks class
  * @author Mikko Hilpinen
  * @since 25.2.2022, v1.15
  */
object TimedTasksTest extends App
{
	DataType.setup()
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("test").executionContext
	
	println("Running TimedTasksTest...")
	
	var waitStart = Now.toInstant
	def testTime(minPassed: FiniteDuration, maxPassed: FiniteDuration) = {
		val passed = Now - waitStart
		assert(passed > minPassed)
		assert(passed < maxPassed)
	}
	
	// Tests finite tasks list and delayed start
	val tasks = new TimedTasks()
	val counter1 = Volatile(0)
	val counter2 = Volatile(0)
	
	waitStart = Now
	tasks.add(Now + 0.5.seconds) {
		val newVal = counter1.updateAndGet { _ + 1 }
		if (newVal < 3)
			Some(Now + 0.5.seconds)
		else
			None
	}
	tasks.add(Now + 0.2.seconds) {
		val newVal = counter2.updateAndGet { _ + 1 }
		if (newVal < 3)
			Some(Now + 1.0.seconds)
		else
			None
	}
	Wait(0.1.seconds)
	assert(tasks.state == NotStarted)
	tasks.runAsync()
	Wait(0.2.seconds) // total 0.3
	assert(tasks.state == Running)
	assert(counter1.value == 0)
	assert(counter2.value == 1)
	Wait(0.3.seconds) // total 0.6
	assert(counter1.value == 1)
	assert(counter2.value == 1)
	Wait(0.5.seconds) // total 1.1
	assert(counter1.value == 2)
	assert(counter2.value == 1)
	Wait(0.5.seconds) // total 1.6
	assert(counter1.value == 3)
	assert(counter2.value == 2)
	assert(tasks.state == Running)
	assert(tasks.completionFuture.waitFor().get == Completed)
	testTime(2.0.seconds, 2.6.seconds)
	assert(counter1.value == 3)
	assert(counter2.value == 3)
	
	// Tests automated restart
	counter1.value = 0
	waitStart = Now
	tasks.addOnce(Now + 0.5.seconds) { counter1.update { _ + 1 } }
	Wait(0.2.seconds)
	assert(tasks.state == Running)
	assert(counter1.value == 0)
	assert(tasks.completionFuture.waitFor().get == Completed)
	testTime(0.4.seconds, 0.7.seconds)
	assert(counter1.value == 1)
	
	// Tests stop()
	counter1.value = 0
	waitStart = Now
	tasks.addOnce(Now + 5.seconds) { counter1.update { _ + 1 } }
	Wait(0.1.seconds)
	assert(tasks.state == Running)
	assert(tasks.stop().waitFor().get == Stopped)
	testTime(0.05.seconds, 0.2.seconds)
	assert(counter1.value == 0)
	
	println("Done!")
}

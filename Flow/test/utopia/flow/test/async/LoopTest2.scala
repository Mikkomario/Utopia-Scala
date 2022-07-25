package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.ProcessState.{Completed, Running, Stopped}
import utopia.flow.async.{CloseHook, LoopingProcess, ThreadPool, Volatile, Wait}
import utopia.flow.generic.DataType
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.WaitTarget.WaitDuration
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

/**
  * Tests LoopingProcess and Loop
  * @author Mikko Hilpinen
  * @since 25.2.2022, v1.15
  */
object LoopTest2 extends App
{
	DataType.setup()
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("test").executionContext
	
	println("Running DelayTest...")
	
	var waitStart = Now.toInstant
	def testTime(minPassed: FiniteDuration, maxPassed: FiniteDuration) = {
		val passed = Now - waitStart
		assert(passed > minPassed)
		assert(passed < maxPassed)
	}
	
	// Tests temporary async looping
	val counter = Volatile(0)
	val l1 = LoopingProcess(0.5.seconds) { _ =>
		val newVal = counter.updateAndGet { _ + 1 }
		if (newVal < 3)
			Some(WaitDuration(0.3.seconds))
		else
			None
	}
	
	waitStart = Now
	l1.runAsync()
	Wait(0.3.seconds)
	assert(l1.state == Running)
	assert(counter.value == 0)
	Wait(0.3.seconds)
	assert(l1.state == Running)
	assert(counter.value == 1)
	Wait(0.3.seconds)
	assert(l1.state == Running)
	assert(counter.value == 2)
	assert(l1.completionFuture.waitFor().get == Completed)
	testTime(1.0.seconds, 1.3.seconds)
	assert(counter.value == 3)
	
	// Tests loop stopping
	counter.value = 0
	waitStart = Now
	l1.runAsync()
	Wait(0.2.seconds)
	assert(l1.state == Running)
	assert(counter.value == 0)
	assert(l1.stop().waitFor().get == Stopped)
	testTime(0.1.seconds, 0.3.seconds)
	assert(counter.value == 0)
	
	// Tests CloseHook interactions
	waitStart = Now
	l1.runAsync()
	Wait(0.1.seconds)
	CloseHook.shutdown()
	testTime(0.05.seconds, 0.4.seconds)
	assert(l1.state == Stopped)
	assert(counter.value == 0)
	
	println("Done!")
}

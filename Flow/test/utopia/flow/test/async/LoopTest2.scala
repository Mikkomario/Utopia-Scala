package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.CloseHook
import utopia.flow.async.process.ProcessState.{Completed, Running, Stopped}
import utopia.flow.async.process.WaitTarget.WaitDuration
import utopia.flow.async.process.{LoopingProcess, Wait}
import utopia.flow.test.TestContext._
import utopia.flow.time.{Duration, Now}
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.Volatile

/**
  * Tests LoopingProcess and Loop
  * @author Mikko Hilpinen
  * @since 25.2.2022, v1.15
  */
object LoopTest2 extends App
{
	println("Running LoopTest...")
	
	var waitStart = Now.toInstant
	def testTime(minPassed: Duration, maxPassed: Duration) = {
		val passed = Now - waitStart
		assert(passed > minPassed)
		assert(passed < maxPassed)
	}
	
	// Tests temporary async looping
	val counter = Volatile(0)
	val l1 = LoopingProcess.restartable.after(0.5.seconds) { _ =>
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
	println("\nTesting loop-stopping")
	counter.value = 0
	waitStart = Now
	assert(l1.state == Completed, l1.state)
	l1.runAsync()
	Wait(0.2.seconds)
	assert(l1.state == Running, l1.state)
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

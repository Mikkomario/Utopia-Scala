package utopia.flow.test.async

import utopia.flow.async.process.ProcessState.NotStarted
import utopia.flow.async.process.{LoopingProcess, Wait}
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.Volatile

/**
  * Tests loop-interrupting / speeding up the next iteration
  * @author Mikko Hilpinen
  * @since 22.11.2023, v2.3
  */
object LoopInterruptionTest extends App
{
	import utopia.flow.test.TestContext._
	
	private val waitLock = new AnyRef
	private val counter = Volatile(0)
	
	private val loop = LoopingProcess(waitLock = waitLock) { _ =>
		counter.update { _ + 1 }
		Some(2.seconds)
	}
	
	println("Starting the test...")
	assert(counter.value == 0)
	assert(loop.state == NotStarted)
	
	loop.runAsync()
	Wait(0.5.seconds)
	assert(counter.value == 1)
	
	loop.skipWait()
	Wait(0.5.seconds)
	assert(counter.value == 2)
	
	Wait(0.5.seconds)
	assert(counter.value == 2)
	
	Wait(1.5.seconds)
	assert(counter.value == 3)
	
	loop.stop()
	Wait(2.5.seconds)
	assert(counter.value == 3)
	
	println("Success!")
}

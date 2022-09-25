package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.{CloseHook, ThreadPool}
import utopia.flow.async.process.ProcessState.{Completed, Running, Stopped}
import utopia.flow.async.process.{DelayedProcess, Wait}
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.async.VolatileFlag

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

/**
  * Tests Delay and DelayedProcess
  * @author Mikko Hilpinen
  * @since 25.2.2022, v1.15
  */
object DelayTest extends App
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
	
	val flag = VolatileFlag()
	
	// Tests asynchronous delay
	val d1 = DelayedProcess(0.5.seconds) { _ => flag.set() }
	
	waitStart = Now
	d1.runAsync()
	Wait(0.2.seconds)
	assert(d1.state == Running)
	assert(flag.isNotSet)
	assert(d1.completionFuture.waitFor().get == Completed)
	testTime(0.4.seconds, 0.6.seconds)
	assert(flag.isSet)
	
	// Tests stop() during delay
	flag.reset()
	waitStart = Now
	d1.runAsync()
	Wait(0.1.seconds)
	assert(d1.state == Running)
	assert(d1.stop().waitFor().get == Stopped)
	testTime(0.05.seconds, 0.2.seconds)
	assert(flag.isNotSet)
	
	// Tests stop() during the function call
	val lock = new AnyRef
	val d2 = DelayedProcess(0.2.seconds, lock) { p =>
		assert(!p.value)
		Wait(0.5.seconds, lock)
		assert(p.value)
		flag.set()
	}
	
	flag.reset()
	waitStart = Now
	d2.runAsync()
	Wait(0.3.seconds)
	assert(d2.state == Running)
	assert(d2.stop().waitFor().get == Stopped)
	testTime(0.2.seconds, 0.4.seconds)
	assert(flag.isSet)
	
	// Test CloseHook interactions
	val flag2 = VolatileFlag()
	val flag3 = VolatileFlag()
	val flag4 = VolatileFlag()
	val d3 = DelayedProcess.rigid(2.0.seconds) { flag2.set() }
	val d4 = DelayedProcess.hurriable(5.0.seconds) { p =>
		assert(p.value)
		flag3.set()
	}
	val d5 = DelayedProcess.skippable(5.0.seconds) { p =>
		assert(!p.value)
		flag4.set()
	}
	
	waitStart = Now
	d3.runAsync()
	d4.runAsync()
	d5.runAsync()
	Wait(0.2.seconds)
	assert(d3.state == Running)
	assert(d4.state == Running)
	assert(d5.state == Running)
	val shutdownFuture = Future { CloseHook.shutdown() }
	Wait(1.0.seconds)
	assert(d3.state == Running)
	assert(d4.state == Completed)
	assert(d5.state == Stopped)
	assert(flag2.isNotSet)
	assert(flag3.isSet)
	assert(flag4.isNotSet)
	shutdownFuture.waitFor().get
	testTime(1.9.seconds, 3.0.seconds)
	assert(d3.state == Completed)
	assert(flag2.isSet)
	
	println("Done!")
}

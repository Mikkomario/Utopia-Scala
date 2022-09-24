package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ThreadPool
import utopia.flow.async.process.WaitUtils
import utopia.flow.time.TimeExtensions._
import utopia.flow.async.process.WaitTarget.Until
import utopia.flow.util.logging.{Logger, SysErrLogger}

import java.time.Instant
import scala.concurrent.ExecutionContext

/**
 * Tests loops
 */
@deprecated("Replaced with LoopTest2", "v1.15")
object LoopTest extends App
{
	// Makes sure WaitTarget / Until is working
	val started = Instant.now()
	val targetTime = started + 1.seconds
	Until(targetTime).waitWith(this)
	val firstWaitDuration = Instant.now() - started
	
	assert(firstWaitDuration < 1100.millis)
	assert(firstWaitDuration > 999.millis)
	
	// Creates execution context
	implicit val logger: Logger = SysErrLogger
	implicit val context: ExecutionContext = new ThreadPool("Test").executionContext
	
	// Creates the loop
	var loopCount = 0
	val loop = Loop(100.millis) { loopCount += 1 }
	
	assert(loopCount == 0)
	
	// Starts the loop, then waits
	loop.startAsync()
	WaitUtils.wait(3.seconds, this)
	val loopCountAfterWait = loopCount
	
	println(loopCountAfterWait)
	assert(loopCountAfterWait > 25)
	assert(loopCountAfterWait < 35)
	
	val completion = loop.stop()
	val loopCountAfterStop = loopCount
	
	WaitUtils.wait(1.seconds, this)
	
	println(loopCount - loopCountAfterStop)
	assert(loopCountAfterStop == loopCount)
	
	completion.waitFor()
	
	println("Success")
}

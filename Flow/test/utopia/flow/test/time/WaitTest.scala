package utopia.flow.test.time

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ThreadPool
import utopia.flow.async.process.{SingleWait, WaitUtils}
import utopia.flow.time.WaitTarget.WaitDuration
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Tests wait related features
 * @author Mikko Hilpinen
 * @since 28.11.2019, v1.6.1+
 */
@deprecated("Replaced with a new version", "v1.15")
object WaitTest extends App
{
	implicit val logger: Logger = SysErrLogger
	private implicit val exc: ExecutionContext = new ThreadPool("Test").executionContext
	
	var singleWaitCompleted = false
	
	def tryCompleteWait() =
	{
		assert(!singleWaitCompleted)
		singleWaitCompleted = true
	}
	
	val singleWait = new SingleWait(WaitDuration(1.seconds))
	
	// Runs wait in background
	val singleWaitFuture = Future {
		singleWait.run()
		tryCompleteWait()
	}
	assert(!singleWaitCompleted)
	val mainLock = new AnyRef
	WaitUtils.wait(0.5.seconds, mainLock)
	assert(!singleWaitCompleted)
	WaitUtils.wait(0.6.seconds, mainLock)
	assert(singleWaitCompleted)
	
	val delayedResult = WaitUtils.delayed(1.seconds) { "Hello World!" }
	assert(delayedResult.isEmpty)
	WaitUtils.wait(1.1.seconds, mainLock)
	assert(delayedResult.isSuccess)
	assert(delayedResult.waitFor().get == "Hello World!")
	
	println("Success!")
}

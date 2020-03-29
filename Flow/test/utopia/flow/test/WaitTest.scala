package utopia.flow.test

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.ThreadPool
import utopia.flow.util.TimeExtensions._
import utopia.flow.util.{SingleWait, WaitUtils}
import utopia.flow.util.WaitTarget.WaitDuration

import scala.concurrent.{ExecutionContext, Future}

/**
 * Tests wait related features
 * @author Mikko Hilpinen
 * @since 28.11.2019, v1.6.1+
 */
object WaitTest extends App
{
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

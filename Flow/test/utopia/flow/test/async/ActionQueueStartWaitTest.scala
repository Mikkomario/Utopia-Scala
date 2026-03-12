package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ActionQueue
import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.flow.test.TestContext._
import utopia.flow.time.{Duration, Now}

/**
 * Tests action start -waiting for ActionQueue
 * @author Mikko Hilpinen
 * @since 11.03.2026, v2.8
 */
object ActionQueueStartWaitTest extends App
{
	// TESTS    ---------------------------
	
	println("Starting")
	
	private val q = ActionQueue()
	private var t0 = Now.toInstant
	
	private val a1 = newAction
	testTime("Start", Duration.zero, 0.1.seconds)
	
	a1.waitUntilStarted().get
	testTime("Started", Duration.zero, 0.1.seconds)
	
	a1.future.waitFor().get
	testTime("Completed", 0.4.seconds, 0.8.seconds)
	
	t0 = Now
	private val a2 = newAction
	private val a3 = newAction
	
	a3.waitUntilStarted().get
	testTime("A3 started", 0.4.seconds, 0.8.seconds)
	assert(a2.isCompleted)
	
	a3.future.waitFor().get
	testTime("A3 completed", 0.9.seconds, 1.4.seconds)
	
	println("Done!")
	
	
	// COMPUTED ---------------------------
	
	private def newAction = q.push { Wait(0.5.seconds) }
	
	
	// OTHER    ---------------------------
	
	private def testTime(phase: String, expectedMin: Duration, expectedMax: Duration) = {
		val d = Now.toInstant - t0
		println(s"${ d.description }: $phase")
		assert(d >= expectedMin && d <= expectedMax)
	}
}

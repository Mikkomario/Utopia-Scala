package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.AccessQueue
import utopia.flow.async.process.{Delay, Wait}
import utopia.flow.test.TestContext._
import utopia.flow.time.{Duration, Now}
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.Volatile

import scala.concurrent.Future

/**
 * Tests AccessQueue functionality
 * @author Mikko Hilpinen
 * @since 12.12.2025, v2.8
 */
object AccessQueueTest extends App
{
	// SETUP    ------------------------
	
	private val delay = 0.5.seconds
	private val blockingDelay = 0.2.seconds
	
	private val p = Volatile(0)
	private val q = new AccessQueue(p)
	
	private val t0 = Now.toInstant
	
	
	// TESTS    ------------------------
	
	private val a1 = access(0, 0.4.seconds, 0.8.seconds)
	assert(!a1.isCompleted)
	
	Wait(0.1.seconds)
	private val a2 = access(1, 0.8.seconds, 1.3.seconds)
	assert(!a1.isCompleted)
	assert(!a2.isCompleted)
	
	Wait(0.1.seconds)
	private val a3 = access(2, 1.3.seconds, 1.8.seconds)
	assert(!a1.isCompleted)
	assert(!a2.isCompleted)
	assert(!a3.isCompleted)
	
	println("Waiting for the queued tests to complete...")
	testCompletion(a1, 0.4.seconds, 0.8.seconds)
	assert(!a2.isCompleted)
	testCompletion(a2, 0.8.seconds, 1.3.seconds)
	assert(!a3.isCompleted)
	testCompletion(a3, 1.3.seconds, 1.8.seconds)
	
	Future { accessBlocking(3, 1.5.seconds, 2.0.seconds) }
	Wait(0.1.seconds)
	Future { accessBlocking(4, 1.7.seconds, 2.2.seconds) }
	Wait(0.1.seconds)
	accessBlocking(5, 1.9.seconds, 2.4.seconds)
	
	println("Done!")
	
	
	// OTHER    ------------------------
	
	private def access(expected: Int, tMin: Duration, tMax: Duration) =
		q { p =>
			println(s"Before $expected: ${ testTime(tMin - delay, tMax - delay) }")
			Delay(delay) {
				val i = p.getAndUpdate { _ + 1 }
				assert(i == expected, s"Expected $expected, got $i")
				println(s"$expected: ${ testTime(tMin, tMax) }")
			}
		}
		
	private def testCompletion(future: Future[Any], tMin: Duration, tMax: Duration) = {
		assert(future.waitFor().isSuccess)
		testTime(tMin, tMax)
	}
	
	private def accessBlocking(expected: Int, tMin: Duration, tMax: Duration) = {
		q.blocking { p =>
			Wait(blockingDelay)
			// WET WET
			val i = p.getAndUpdate { _ + 1 }
			assert(i == expected, s"Expected $expected, got $i")
			println(s"$expected (blocking): ${ testTime(tMin, tMax) }")
		}
	}
	
	private def testTime(tMin: Duration, tMax: Duration) = {
		val d = Now - t0
		assert(d >= tMin, s"${ d.description } < ${ tMin.description }")
		assert(d <= tMax, s"${ d.description } > ${ tMax.description }")
		d.description
	}
}

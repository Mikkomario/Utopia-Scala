package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.MapParallel
import utopia.flow.async.context.{AccessQueue, ActionQueue}
import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.flow.test.TestContext._
import utopia.flow.time.{Duration, Now}

/**
 * Tests ParallelBuilder
 * @author Mikko Hilpinen
 * @since 12.12.2025, v2.8
 */
object ParallelBuilderTest extends App
{
	// SETUP   --------------------
	
	private val delay = 0.5.seconds
	private val range = 0.3.seconds
	
	private val q = ActionQueue(4)
	private val qq = new AccessQueue(q)
	
	private val t0 = Now.toInstant
	private val b = MapParallel.sync
		.map { i: Int =>
			Wait(delay)
			println(s"$i: ${ (Now - t0).description }")
			i + 100
		}
		.toBuilderUsing(qq, 4)
	
	
	// TESTS    ------------------
	
	b += 1
	b += 2
	b += 3
	
	testTime(0.seconds)
	assert(!q.containsPendingActions)
	assert(q.nonEmpty)
	
	b += 4
	b += 5
	testTime(0.seconds)
	assert(q.containsPendingActions)
	assert(q.pendingCount == 1)
	
	Wait(0.1.seconds)
	
	b ++= (6 to 7)
	testTime(0.1.seconds)
	
	b += 8
	testTime(0.1.seconds)
	assert(q.pendingCount == 4)
	
	b += 9
	testTime(0.5.seconds)
	assert(q.pendingCount == 1, q.pendingCount)
	
	b ++= (10 to 12)
	testTime(0.5.seconds)
	assert(q.pendingCount == 4)
	
	b ++= (13 to 24)
	testTime(2.0.seconds)
	assert(q.pendingCount == 4)
	
	private val r = b.result().waitForResult().get
	assert(r.size == 24)
	testTime(3.0.seconds)
	assert(r.contains(101))
	assert(r.contains(102))
	assert(r.contains(124))
	assert(q.isEmpty)
	println(s"[${ r.mkString(", ") }]: ${ (Now - t0).description }")
	
	println("Done!")
	
	
	// OTHER    ------------------
	
	private def testTime(target: Duration) = {
		val d = Now - t0
		val tMin = target - range
		assert(d >= tMin, s"${ d.description } < ${ tMin.description }")
		val tMax = target + range
		assert(d <= tMax, s"${ d.description } > ${ tMax.description }")
	}
}

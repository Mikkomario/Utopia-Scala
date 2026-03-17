package utopia.flow.test.async

import utopia.flow.async.context.MappingFunnel
import utopia.flow.async.process.{Delay, Wait}
import utopia.flow.time.TimeExtensions._
import utopia.flow.operator.Identity
import utopia.flow.test.TestContext._
import utopia.flow.time.Now
import utopia.flow.test

import java.time.Instant

/**
 * Tests MappingFunnel
 * @author Mikko Hilpinen
 * @since 17.03.2026, v2.8.1
 */
object MappingFunnelTest extends App
{
	// TESTS    -----------------------
	
	private var t0: Instant = Now.toInstant
	private val funnel = MappingFunnel[Double, Double](1) { _ - 0.001 } { a =>
		println(s"Running $a")
		Delay(a.seconds)(a)
	}
	
	// Tests with a single task
	println("\nTesting 1 task")
	private val r1 = funnel.push(0.2)
	
	Wait(0.1.seconds)
	assert(!r1.isCompleted, (Now - t0).description)
	
	Wait(0.15.seconds)
	assert(r1.isCompleted, (Now - t0).description)
	
	// Tests with 2 parallel tasks + one queued task
	println("\nTesting 2+1 tasks")
	t0 = Now
	private val r2 = funnel.push(0.5)
	private val r3 = funnel.push(0.25)
	private val r4 = funnel.push(0.4)
	
	Wait(0.1.seconds)
	assert(!r2.isCompleted, (Now - t0).description)
	assert(!r3.isCompleted, (Now - t0).description)
	assert(!r4.isCompleted, (Now - t0).description)
	
	Wait(0.2.seconds)
	assert(!r2.isCompleted, (Now - t0).description)
	assert(r3.isCompleted, (Now - t0).description)
	assert(!r4.isCompleted, (Now - t0).description)
	
	Wait(0.3.seconds)
	assert(r2.isCompleted, (Now - t0).description)
	assert(!r4.isCompleted, (Now - t0).description)
	
	Wait(0.1.seconds)
	assert(r4.isCompleted, (Now - t0).description)
	
	// Tests queue-resolve ordering / optimization
	println("\nTesting 1+4 tasks")
	t0 = Now
	private val r5 = funnel.push(1.0)
	private val r6 = funnel.push(0.2)
	private val r7 = funnel.push(0.8)
	private val r8 = funnel.push(0.1)
	private val r9 = funnel.push(0.5)
	
	Wait(0.8.seconds)
	assert(!r5.isCompleted, (Now - t0).description)
	assert(!r6.isCompleted, (Now - t0).description)
	assert(!r7.isCompleted, (Now - t0).description)
	assert(!r8.isCompleted, (Now - t0).description)
	assert(!r9.isCompleted, (Now - t0).description)
	
	Wait(0.25.seconds) // 1.05 - First task should be run
	assert(r5.isCompleted, (Now - t0).description)
	assert(!r6.isCompleted, (Now - t0).description)
	assert(!r7.isCompleted, (Now - t0).description)
	assert(!r8.isCompleted, (Now - t0).description)
	assert(!r9.isCompleted, (Now - t0).description)
	
	Wait(0.1.seconds) // 1.15 - 0.8 and 0.2 should be running
	assert(!r6.isCompleted, (Now - t0).description)
	assert(!r7.isCompleted, (Now - t0).description)
	assert(!r8.isCompleted, (Now - t0).description)
	assert(!r9.isCompleted, (Now - t0).description)
	
	Wait(0.1.seconds) // 1.25 - 0.2 should have completed, 0.1 should have started
	assert(r6.isCompleted, (Now - t0).description)
	assert(!r7.isCompleted, (Now - t0).description)
	assert(!r8.isCompleted, (Now - t0).description)
	assert(!r9.isCompleted, (Now - t0).description)
	
	Wait(0.1.seconds) // 1.35 - 0.1 should have completed
	assert(!r7.isCompleted, (Now - t0).description)
	assert(r8.isCompleted, (Now - t0).description)
	assert(!r9.isCompleted, (Now - t0).description)
	
	Wait(0.5.seconds) // 1.85 - 0.8 should have completed and 0.5 started
	assert(r7.isCompleted, (Now - t0).description)
	assert(!r9.isCompleted, (Now - t0).description)
	
	Wait(0.2.seconds) // 2.05 - 0.5 should be running
	assert(!r9.isCompleted, (Now - t0).description)
	
	Wait(0.3.seconds) // 2.35 - 0.5 should have completed
	assert(r9.isCompleted, (Now - t0).description)
	
	// Tests ordered funnel
	println("\nTesting ordered")
	t0 = Now
	private val ordered = MappingFunnel[Double, Double](1, ordered = true) { _ - 0.001 } { a =>
		println(s"Running $a")
		Delay(a.seconds)(a)
	}
	private val r10 = ordered.push(0.3)
	private val r11 = ordered.push(0.8)
	private val r12 = ordered.push(0.1)
	private val r13 = ordered.push(0.2)
	
	Wait(0.2.seconds)
	assert(!r10.isCompleted)
	assert(!r11.isCompleted)
	assert(!r12.isCompleted)
	assert(!r13.isCompleted)
	
	Wait(0.15.seconds) // 0.35 - 0.3 should have finished, and 0.8 and 0.1 started
	assert(r10.isCompleted)
	assert(!r11.isCompleted)
	assert(!r12.isCompleted)
	assert(!r13.isCompleted)
	
	Wait(0.1.seconds) // 0.45 - 0.8 should be running, 0.1 completed and 0.2 started
	assert(!r11.isCompleted)
	assert(r12.isCompleted)
	assert(!r13.isCompleted)
	
	Wait(0.2.seconds) // 0.65 - 0.2 should have just completed
	assert(!r11.isCompleted)
	assert(r13.isCompleted)
	
	Wait(0.3.seconds) // 0.95 - 0.8 should still be running
	assert(!r11.isCompleted)
	
	Wait(0.2.seconds) // 1.15 - 0.8 should have just finished
	assert(r11.isCompleted)
	
	println("Success!")
}

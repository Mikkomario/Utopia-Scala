package utopia.flow.test.async

import utopia.flow.async.context.MappingFunnel
import utopia.flow.async.process.{Delay, Wait}
import utopia.flow.test.TestContext._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.NumberExtensions._

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
	// Expects 0.3 to start immediately
	private val r10 = ordered.push(0.3)
	private val r11 = ordered.push(0.8)
	private val r12 = ordered.push(0.1)
	private val r13 = ordered.push(0.2)
	
	Wait(0.2.seconds) // 0.2 - Nothing should have completed
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
	
	
	// Tests a version which prioritizes larger tasks
	
	println("\nTesting prioritizing")
	private val f2 = MappingFunnel[Double, Double](1, prioritizeLarger = true) { _ - 0.001 } { a =>
		println(s"Running ${ a.roundDecimals(1) }")
		Delay(a.seconds)(a)
	}
	
	// Expects 0.7 and 0.2 to start immediately
	t0 = Now
	private val r14 = f2.push(0.7)
	private val r15 = f2.push(0.8)
	private val r16 = f2.push(0.3)
	private val r17 = f2.push(0.2)
	
	Wait(0.25.seconds) // 0.25 - Expects 0.2 to have completed; 0.3 should not start.
	assert(r17.isCompleted)
	
	Wait(0.1.seconds) // 0.35 - 0.3 should not have started, nor completed yet
	assert(!r16.isCompleted)
	
	Wait(0.2.seconds) // 0.55 - 0.3 should not have started, nor completed
	assert(!r16.isCompleted)
	assert(!r15.isCompleted)
	
	Wait(0.2.seconds) // 0.75 - 0.7 should have just completed; Expects 0.8 to start next.
	assert(r14.isCompleted)
	assert(!r16.isCompleted)
	assert(!r15.isCompleted)
	
	Wait(0.05.seconds) // 0.8 - Adds 3 small tasks; Expects 2 of them to start immediately
	private val r18 = f2.push(0.1)
	private val r19 = f2.push(0.1)
	private val r20 = f2.push(0.1)
	
	Wait(0.15.seconds) // 0.95 - 2/3 0.1s should have completed
	assert(r18.isCompleted)
	assert(r19.isCompleted)
	assert(!r20.isCompleted)
	assert(!r16.isCompleted)
	assert(!r15.isCompleted)
	
	Wait(0.1.seconds) // 1.05 - 3/3 0.1s should have completed
	assert(r20.isCompleted)
	assert(!r16.isCompleted)
	assert(!r15.isCompleted)
	
	Wait(0.5.seconds) // 1.55 - 0.8 should have just completed and 0.3 started
	assert(r15.isCompleted)
	assert(!r16.isCompleted)
	
	Wait(0.3.seconds) // 1.85 - 0.3 should have completed
	assert(r16.isCompleted)
	
	println("\nTesting prioritizing 2 (smaller chunks first)")
	// Expects 0.1, 0.2, 0.3 and 0.4 to start immediately (total 1.0)
	t0 = Now
	private val r21 = f2.push(0.1)
	private val r22 = f2.push(0.2)
	private val r23 = f2.push(0.3)
	private val r24 = f2.push(0.8)
	private val r25 = f2.push(0.4)
	private val r26 = f2.push(0.6)
	
	private val r27 = f2.push(0.1)
	private val r28 = f2.push(0.2)
	private val r29 = f2.push(0.3)
	private val r30 = f2.push(0.4)
	
	Wait(0.15.seconds) // 0.15 - 0.1 should have completed, nothing started (total 0.9)
	assert(r21.isCompleted)
	assert(!r22.isCompleted)
	assert(!r23.isCompleted)
	assert(!r27.isCompleted)
	
	Wait(0.1.seconds) // 0.25 - 0.2 should have completed, nothing started (total 0.7)
	assert(r22.isCompleted)
	assert(!r23.isCompleted)
	assert(!r27.isCompleted)
	assert(!r28.isCompleted)
	
	Wait(0.1.seconds) // 0.35 - 0.3 should have completed, 0.6 started (total 1.0)
	assert(r23.isCompleted)
	assert(!r25.isCompleted)
	assert(!r27.isCompleted)
	assert(!r28.isCompleted)
	assert(!r29.isCompleted)
	
	Wait(0.1.seconds) // 0.45 - 0.4 should have completed, 0.2 started (total 0.8)
	assert(r25.isCompleted)
	assert(!r27.isCompleted)
	assert(!r28.isCompleted)
	assert(!r29.isCompleted)
	assert(!r30.isCompleted)
	
	Wait(0.1.seconds) // 0.55 - No change expected (0.6 and 0.2 should be running)
	assert(!r27.isCompleted)
	assert(!r28.isCompleted)
	assert(!r29.isCompleted)
	assert(!r30.isCompleted)
	
	Wait(0.1.seconds) // 0.65 - 0.2 should have just completed and 0.1 started, 0.6 should still be running (total 0.7)
	assert(r28.isCompleted)
	assert(!r26.isCompleted)
	assert(!r27.isCompleted)
	assert(!r29.isCompleted)
	assert(!r30.isCompleted)
	
	Wait(0.1.seconds) // 0.75 - 0.1 should have just completed, nothing started; [0.6]
	assert(r27.isCompleted)
	assert(!r26.isCompleted)
	assert(!r29.isCompleted)
	assert(!r30.isCompleted)
	
	Wait(0.2.seconds) // 0.95 - 0.6 should have just completed and 0.8 started; [0.8]
	assert(r26.isCompleted)
	assert(!r24.isCompleted)
	assert(!r29.isCompleted)
	assert(!r30.isCompleted)
	
	Wait(0.7.seconds) // 1.65 - 0.8 should be about to complete [0.8] < [0.4, 0.3]
	assert(!r24.isCompleted)
	assert(!r29.isCompleted)
	assert(!r30.isCompleted)
	
	Wait(0.1.seconds) // 1.75 - 0.8 should have completed, and 0.4 and 0.3 started
	assert(r24.isCompleted)
	assert(!r29.isCompleted)
	assert(!r30.isCompleted)
	
	Wait(0.2.seconds) // 1.95 - 0.3 should be about to complete
	assert(!r29.isCompleted)
	assert(!r30.isCompleted)
	
	Wait(0.1.seconds) // 0.3 should have completed
	assert(r29.isCompleted)
	assert(!r30.isCompleted)
	
	Wait(0.1.seconds) // 0.4 should have completed
	assert(r30.isCompleted)
	
	println("Success!")
}

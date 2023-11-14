package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.TwoThreadBuffer
import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.UncertainNumber

import scala.concurrent.Future

/**
  * Tests the [[utopia.flow.async.context.TwoThreadBuffer]] class
  * @author Mikko Hilpinen
  * @since 14.11.2023, v2.3
  */
object TwoThreadBufferTest extends App
{
	import utopia.flow.test.TestContext._
	
	// Creates the buffer
	println("Starting...")
	val buffer = new TwoThreadBuffer[Int](3)
	
	assert(!buffer.output.immediately.hasNext)
	assert(buffer.isCurrentlyEmpty)
	
	// Queues hasNext (output stuck)
	println("Queues hasNext...")
	val hasNext1 = Future { buffer.output.hasNext }
	Wait(1.0.seconds)
	
	assert(hasNext1.isEmpty)
	assert(buffer.input.immediately.hasCapacity)
	assert(buffer.isNotCurrentlyFull)
	assert(buffer.isCurrentlyEmpty)
	
	// Appends 3 items
	println("Appends 3 items")
	buffer.input.push(Vector(1, 2, 3))
	
	assert(buffer.input.immediately.isFull)
	assert(buffer.isCurrentlyFull)
	assert(buffer.isNotCurrentlyEmpty)
	
	Wait(1.0.seconds)
	assert(hasNext1.isCompleted)
	assert(hasNext1.waitFor().get)
	
	// Reads the items one by one
	println("Reads the 3 items")
	assert(buffer.output.next() == 1)
	
	assert(buffer.input.immediately.hasCapacity)
	assert(buffer.isNotCurrentlyFull)
	
	assert(buffer.output.next() == 2)
	assert(buffer.output.next() == 3)
	
	assert(!buffer.output.immediately.hasNext)
	
	// Queues next item read (output stuck)
	println("Queues read 4")
	val next4 = Future { buffer.output.next() }
	Wait(1.0.seconds)
	
	assert(next4.isEmpty)
	
	// Adds one item to the buffer
	println("Appends 4th item")
	buffer.input.push(4)
	
	Wait(1.0.seconds)
	assert(next4.isCompleted)
	assert(next4.waitFor().get == 4)
	
	// Overfills the buffer by 2 (input stuck)
	println("Attempts to overfill the buffer")
	val push5to9 = Future { buffer.input.push(Vector(5, 6, 7, 8, 9)) }
	Wait(1.0.seconds)
	
	assert(push5to9.isEmpty)
	assert(buffer.isCurrentlyFull)
	assert(buffer.output.immediately.size == 3)
	
	// Reads one element (input still stuck)
	println("Reads one element")
	assert(buffer.output.hasNext)
	assert(buffer.output.next() == 5)
	
	Wait(1.0.seconds)
	assert(buffer.output.immediately.size == 3)
	assert(push5to9.isEmpty)
	assert(buffer.input.immediately.isFull)
	
	// Reads one element (push completes)
	println("Reads one more element")
	assert(buffer.output.next() == 6)
	
	Wait(1.0.seconds)
	assert(push5to9.isCompleted)
	assert(buffer.output.immediately.size == 3)
	assert(buffer.isCurrentlyFull)
	
	// Attempts to push immediately and fails
	println("Attempts push")
	assert(buffer.input.immediately.push(10).isDefined)
	
	// Immediately reads 2 elements
	println("Reads 2 elements")
	assert(buffer.output.immediately.collectNext(2) == Vector(7, 8))
	
	assert(buffer.input.immediately.availableCapacity == 2)
	assert(buffer.output.immediately.size == 1)
	
	// Declares that there will be more input
	println("Declares 1+ more input")
	buffer.input.remainingSize = UncertainNumber.positive
	
	assert((buffer.output.sizeEstimate > 1).isCertainlyTrue, buffer.output.sizeEstimate)
	
	// Reads the next input
	println("Reads next input")
	assert(buffer.output.next() == 9)
	
	assert(buffer.output.sizeEstimate.isCertainlyPositive)
	assert(buffer.output.immediately.isEmpty)
	
	println(s"Tests hasNext (size estimate = ${buffer.output.sizeEstimate}, buffer size = ${buffer.output.immediately.size})")
	assert(buffer.output.hasNext)
	
	assert(buffer.output.sizeEstimate.isCertainlyPositive)
	assert(buffer.output.immediately.isEmpty)
	
	// Reads the next input (output stuck)
	println("Reads more input")
	val read10 = Future { buffer.output.next() }
	Wait(1.0.seconds)
	
	assert(read10.isEmpty)
	
	// Immediately pushes a value
	println("Pushes")
	assert(buffer.input.immediately.push(10).isEmpty)
	
	Wait(1.0.seconds)
	assert(read10.isCompleted)
	assert(read10.waitFor().get == 10)
	assert(buffer.output.sizeEstimate.isPositive.isUncertain)
	
	// Declares that there will be more values
	println("Declares more input for now")
	buffer.input.declareNotClosing()
	
	assert(buffer.isStillFilling.isCertainlyTrue)
	assert(buffer.output.hasNext)
	assert(!buffer.output.immediately.hasNext)
	
	// Pushes and reads that value + ends declaration of not closing
	println("Processes 1 value, declares maybe input")
	buffer.input.declarePossiblyClosing()
	buffer.input.push(11)
	assert(buffer.output.next() == 11)
	
	// Queues hasNext (output stuck)
	println("Queues hasNext")
	val hasNext2 = Future { buffer.output.hasNext }
	Wait(1.0.seconds)
	val closeFuture = buffer.closedFuture
	
	assert(hasNext2.isEmpty)
	assert(closeFuture.isEmpty)
	
	// Closes input
	println("Closes input")
	buffer.input.close()
	
	Wait(1.0.seconds)
	assert(hasNext2.isCompleted)
	assert(!hasNext2.waitFor().get)
	assert(closeFuture.isCompleted)
	
	println("Success!")
}

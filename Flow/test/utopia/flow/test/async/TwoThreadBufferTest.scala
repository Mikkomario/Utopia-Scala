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
	
	assert(!buffer.input.immediately.hasNext)
	assert(buffer.isCurrentlyEmpty)
	
	// Queues hasNext (output stuck)
	println("Queues hasNext...")
	val hasNext1 = Future { buffer.input.hasNext }
	Wait(1.0.seconds)
	
	assert(hasNext1.isEmpty)
	assert(buffer.output.immediately.hasCapacity)
	assert(buffer.isNotCurrentlyFull)
	assert(buffer.isCurrentlyEmpty)
	
	// Appends 3 items
	println("Appends 3 items")
	buffer.output.push(Vector(1, 2, 3))
	
	assert(buffer.output.immediately.isFull)
	assert(buffer.isCurrentlyFull)
	assert(buffer.isNotCurrentlyEmpty)
	
	Wait(1.0.seconds)
	assert(hasNext1.isCompleted)
	assert(hasNext1.waitFor().get)
	
	// Reads the items one by one
	println("Reads the 3 items")
	assert(buffer.input.next() == 1)
	
	assert(buffer.output.immediately.hasCapacity)
	assert(buffer.isNotCurrentlyFull)
	
	assert(buffer.input.next() == 2)
	assert(buffer.input.next() == 3)
	
	assert(!buffer.input.immediately.hasNext)
	
	// Queues next item read (output stuck)
	println("Queues read 4")
	val next4 = Future { buffer.input.next() }
	Wait(1.0.seconds)
	
	assert(next4.isEmpty)
	
	// Adds one item to the buffer
	println("Appends 4th item")
	buffer.output.push(4)
	
	Wait(1.0.seconds)
	assert(next4.isCompleted)
	assert(next4.waitFor().get == 4)
	
	// Overfills the buffer by 2 (input stuck)
	println("Attempts to overfill the buffer")
	val push5to9 = Future { buffer.output.push(Vector(5, 6, 7, 8, 9)) }
	Wait(1.0.seconds)
	
	assert(push5to9.isEmpty)
	assert(buffer.isCurrentlyFull)
	assert(buffer.input.immediately.size == 3)
	
	// Reads one element (input still stuck)
	println("Reads one element")
	assert(buffer.input.hasNext)
	assert(buffer.input.next() == 5)
	
	Wait(1.0.seconds)
	assert(buffer.input.immediately.size == 3)
	assert(push5to9.isEmpty)
	assert(buffer.output.immediately.isFull)
	
	// Reads one element (push completes)
	println("Reads one more element")
	assert(buffer.input.next() == 6)
	
	Wait(1.0.seconds)
	assert(push5to9.isCompleted)
	assert(buffer.input.immediately.size == 3)
	assert(buffer.isCurrentlyFull)
	
	// Attempts to push immediately and fails
	println("Attempts push")
	assert(buffer.output.immediately.push(10).isDefined)
	
	// Immediately reads 2 elements
	println("Reads 2 elements")
	assert(buffer.input.immediately.collectNext(2) == Vector(7, 8))
	
	assert(buffer.output.immediately.availableCapacity == 2)
	assert(buffer.input.immediately.size == 1)
	
	// Declares that there will be more input
	println("Declares 1+ more input")
	buffer.output.remainingSize = UncertainNumber.positive
	
	assert((buffer.input.sizeEstimate > 1).isCertainlyTrue, buffer.input.sizeEstimate)
	
	// Reads the next input
	println("Reads next input")
	assert(buffer.input.next() == 9)
	
	assert(buffer.input.sizeEstimate.isCertainlyPositive)
	assert(buffer.input.immediately.isEmpty)
	
	println(s"Tests hasNext (size estimate = ${buffer.input.sizeEstimate}, buffer size = ${buffer.input.immediately.size})")
	assert(buffer.input.hasNext)
	
	assert(buffer.input.sizeEstimate.isCertainlyPositive)
	assert(buffer.input.immediately.isEmpty)
	
	// Reads the next input (output stuck)
	println("Reads more input")
	val read10 = Future { buffer.input.next() }
	Wait(1.0.seconds)
	
	assert(read10.isEmpty)
	
	// Immediately pushes a value
	println("Pushes")
	assert(buffer.output.immediately.push(10).isEmpty)
	
	Wait(1.0.seconds)
	assert(read10.isCompleted)
	assert(read10.waitFor().get == 10)
	assert(buffer.input.sizeEstimate.isPositive.isUncertain)
	
	// Declares that there will be more values
	println("Declares more input for now")
	buffer.output.declareNotClosing()
	
	assert(buffer.isStillFilling.isCertainlyTrue)
	assert(buffer.input.hasNext)
	assert(!buffer.input.immediately.hasNext)
	
	// Pushes and reads that value + ends declaration of not closing
	println("Processes 1 value, declares maybe input")
	buffer.output.declarePossiblyClosing()
	buffer.output.push(11)
	assert(buffer.input.next() == 11)
	
	// Queues hasNext (output stuck)
	println("Queues hasNext")
	val hasNext2 = Future { buffer.input.hasNext }
	Wait(1.0.seconds)
	val closeFuture = buffer.closedFuture
	
	assert(hasNext2.isEmpty)
	assert(closeFuture.isEmpty)
	
	// Closes input
	println("Closes input")
	buffer.output.close()
	
	Wait(1.0.seconds)
	assert(hasNext2.isCompleted)
	assert(!hasNext2.waitFor().get)
	assert(closeFuture.isCompleted)
	
	println("Success!")
}

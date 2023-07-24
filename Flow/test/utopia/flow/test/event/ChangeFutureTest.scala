package utopia.flow.test.event

import utopia.flow.async.process.{Delay, Wait}
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.immutable.eventful.ChangeFuture
import utopia.flow.view.mutable.eventful.PointerWithEvents

/**
  * A test for certain functions in Changing and ChangingLike
  * @author Mikko Hilpinen
  * @since 22.9.2022, v1.17
  */
object ChangeFutureTest extends App
{
	import utopia.flow.test.TestContext._
	
	val delay = 0.5.seconds
	
	val original = new PointerWithEvents(1)
	val delayMapped = original.flatMap { i => ChangeFuture(i, Delay(delay) { i + 1 }) }
	
	original.addListener { e => println(s"$Now original: $e") }
	delayMapped.addListener { e => println(s"$Now mapped: $e") }
	
	assert(original.value == 1)
	assert(delayMapped.value == 1)
	
	println("Waits")
	Wait(delay * 1.5)
	
	println("Testing if map completed")
	assert(original.value == 1)
	assert(delayMapped.value == 2)
	
	println("Setting original value to 5")
	original.value = 5
	
	assert(original.value == 5)
	assert(delayMapped.value == 5)
	
	println("Waits")
	Wait(delay * 1.5)
	
	println("Testing if map completed")
	assert(delayMapped.value == 6)
	
	println("Done!")
}

package utopia.flow.test.event

import utopia.flow.async.{ChangeFuture, Delay, Wait}
import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.flow.generic.DataType
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

/**
  * A test for certain functions in Changing and ChangingLike
  * @author Mikko Hilpinen
  * @since 22.9.2022, v1.17
  */
object ChangingTest extends App
{
	DataType.setup()
	import utopia.flow.test.TestContext._
	
	val delay = 0.5.seconds
	
	val original = new PointerWithEvents(1)
	val delayMapped = original.flatMap { i => ChangeFuture.wrap(Delay(delay) { i + 1 }, i) }
	
	original.addListener { e => println(s"$Now original: $e") }
	delayMapped.addListener { e => println(s"$Now mapped: $e") }
	
	assert(original.value == 1)
	assert(delayMapped.value == 1)
	
	Wait(delay * 1.5)
	
	assert(original.value == 1)
	assert(delayMapped.value == 2)
	
	original.value = 5
	
	assert(original.value == 5)
	assert(delayMapped.value == 5)
	
	Wait(delay * 1.5)
	
	assert(delayMapped.value == 6)
	
	println("Done!")
}

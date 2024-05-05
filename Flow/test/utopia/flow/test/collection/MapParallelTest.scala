package utopia.flow.test.collection

import utopia.flow.test.TestContext._
import utopia.flow.async.AsyncCollectionExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.async.process.Wait
import utopia.flow.time.Now

/**
  * Tests parallel mapping
  * @author Mikko Hilpinen
  * @since 04.05.2024, v2.4
  */
object MapParallelTest extends App
{
	println("Starting mapping. Estimated completion in 10 seconds...")
	val startTime = Now.toInstant
	val result = (0 until 100).toVector.mapParallel(5) { i =>
		Wait(0.5.seconds)
		i
	}
	val duration = Now - startTime
	
	assert(result.size == 100)
	assert(result.head == 0)
	assert(result.last == 99)
	
	println(s"Processing took ${ duration.description }")
	
	assert(duration > 5.seconds)
	assert(duration < 15.seconds)
	
	println("Success")
}

package utopia.flow.test.collection

import utopia.flow.async.process.Wait
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.test.TestContext._

import scala.util.Random

/**
  * Tests parallel mapping
  * @author Mikko Hilpinen
  * @since 30.12.2024, v2.5.1
  */
object MapAllParallelTest extends App
{
	println("Starting parallel processing. Estimated completion in 3 seconds")
	private val startTime = Now.toInstant
	private val result = (0 until 30).toVector.mapAllParallel { i =>
		Wait((Random.nextDouble() * 6).seconds)
		i
	}
	private val duration = Now - startTime
	println(s"Complete. Took ${ duration.description }")
	
	assert(result.size == 30)
	assert(result.head == 0)
	assert(result.last == 29)
	
	assert(duration > 1.seconds)
	assert(duration < 8.seconds)
}

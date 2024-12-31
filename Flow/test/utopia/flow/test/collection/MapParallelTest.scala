package utopia.flow.test.collection

import utopia.flow.test.TestContext._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.async.process.Wait
import utopia.flow.time.Now

import scala.util.Random

/**
  * Tests parallel mapping
  * @author Mikko Hilpinen
  * @since 04.05.2024, v2.4
  */
object MapParallelTest extends App
{
	println("Starting mapping. Estimated completion in around 25 seconds...")
	val startTime = Now.toInstant
	val result = (0 until 10000).toVector.mapParallel(20) { i =>
		Wait((Random.nextDouble() * 0.1).seconds)
		i
	}
	val duration = Now - startTime
	println(s"Processing took ${ duration.description }")
	
	assert(result.size == 10000)
	assert(result.head == 0)
	assert(result.last == 9999)
	
	assert(duration > 15.seconds)
	assert(duration < 40.seconds)
	
	println("Success")
}

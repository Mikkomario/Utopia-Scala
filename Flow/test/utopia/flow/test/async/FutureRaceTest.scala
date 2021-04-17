package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.ThreadPool
import utopia.flow.time.WaitUtils
import utopia.flow.time.TimeExtensions._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Tests future race feature
 * @author Mikko Hilpinen
 * @since 11.11.2019, v1.6.1+
 */
object FutureRaceTest extends App
{
	implicit val exc: ExecutionContext = new ThreadPool("test-main").executionContext
	val immediate = Future { 1 }
	immediate.waitFor()
	
	val short = Future { WaitUtils.wait(1.seconds, new AnyRef); 2 }
	val long = Future { WaitUtils.wait(3.seconds, new AnyRef); 3 }
	
	// Case 1: first one completed already
	assert(immediate.raceWith(short).waitFor().get == 1)
	assert(short.raceWith(immediate).waitFor().get == 1)
	
	// Case 2: Waiting for two futures
	assert(short.raceWith(long).waitFor().get == 2)
	
	println("Success!")
}

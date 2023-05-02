package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ThreadPool
import utopia.flow.async.process
import utopia.flow.async.process.Delay
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Tests future race feature
 * @author Mikko Hilpinen
 * @since 11.11.2019, v1.6.1+
 */
object FutureRaceTest extends App
{
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("test-main")
	val immediate = Future { 1 }
	immediate.waitFor()
	
	val short = Delay(1.seconds) { 2 }
	val long = process.Delay(3.seconds) { 3 }
	
	// Case 1: first one completed already
	assert(immediate.raceWith(short).waitFor().get == 1)
	assert(short.raceWith(immediate).waitFor().get == 1)
	
	// Case 2: Waiting for two futures
	assert(short.raceWith(long).waitFor().get == 2)
	
	println("Success!")
}

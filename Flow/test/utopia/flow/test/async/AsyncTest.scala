package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.mutable.async.VolatileList
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.{ExecutionContext, Future}

/**
 * This app tests some asynchronous functions
 */
object AsyncTest extends App
{
	// Creates the thread pool and the execution context
	implicit val logger: Logger = SysErrLogger
	implicit val context: ExecutionContext = new ThreadPool("test-main", 3, 6,
		2.seconds).executionContext
	
	val starts = VolatileList[Int]()
	val ends = VolatileList[Int]()
	
	// Function for starting asynchronous processes
	def makeFuture(index: Int) = Future {
		println(s"Starting future $index")
		starts :+= index
		Wait(2.seconds)
		println(s"Finishing future $index")
		ends :+= index
	}
	
	// Starts 20 asynchronous processes. Only 6 of them should run at any time
	val futures = (1 to 20).map(makeFuture)
	
	// Waits until all of the futures have completed
	println("Waiting for all futures to complete")
	futures.foreach { _.waitFor() }
	
	println("All futures completed, checks results")
	
	val finalStarts = starts.value.sorted
	val finalEnds = ends.value.sorted
	
	println("Started:")
	println(finalStarts.map { _.toString() }.reduce { _ + ", " + _ })
	println("Ended:")
	println(finalEnds.map { _.toString() }.reduce { _ + ", " + _ })
	
	assert(finalStarts.size == 20)
	assert(finalEnds.size == 20)
	
	// Tests futures with timeouts
	val originalFuture = makeFuture(21)
	val shortTimeoutFuture = originalFuture.withTimeout(0.5.seconds)
	val longTimeoutFuture = originalFuture.withTimeout(10.seconds)
	
	assert(shortTimeoutFuture.waitForResult().isFailure)
	println("Short timeout future completed")
	assert(longTimeoutFuture.waitForResult().isSuccess)
	println("Long timeout future completed")
	
	println("Success!")
}

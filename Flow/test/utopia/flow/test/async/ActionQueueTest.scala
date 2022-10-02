package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.{ActionQueue, ThreadPool}
import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.collection.immutable.VectorBuilder
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

/**
 * This class tests ActionQueue under continuous supply of actions
 * @author Mikko Hilpinen
 * @since 22.5.2019, v1.4.1+
 */
object ActionQueueTest extends App
{
	// Tests with limited thread availablility
	implicit val logger: Logger = SysErrLogger
	implicit val context: ExecutionContext = new ThreadPool("Test", 2, 4).executionContext
	
	val queue = new ActionQueue(5)
	
	val genLock = new AnyRef()
	var generatedItems = 0
	val maxGenItems = 1000
	val random = new Random()
	val completionsBuffer = new VectorBuilder[Future[Unit]]()
	
	def task() = Wait(100.millis)
	
	println("Starting tasks")
	
	// Starts generating actions
	while (generatedItems < maxGenItems) {
		completionsBuffer += queue.push { task() }
		generatedItems += 1
		Wait(random.nextInt(10).millis, genLock)
	}
	
	// Checks the completions
	val completions = completionsBuffer.result()
	assert(completions.size == maxGenItems)
	
	println("All completions created")
	println(s"${completions.count { _.isCompleted }} / $maxGenItems items completed")
	
	val successes = completions.waitForSuccesses()
	assert(successes.size == maxGenItems)
	println("All completed!")
}

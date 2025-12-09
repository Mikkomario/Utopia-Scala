package utopia.flow.test.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ActionQueue.QueuedAction
import utopia.flow.async.context.{ActionQueue, ThreadPool}
import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger, TimeLogger}

import scala.collection.immutable.VectorBuilder
import scala.concurrent.ExecutionContext
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
	implicit val context: ExecutionContext = new ThreadPool("Test", 2, 4)
	
	val queue = ActionQueue(5)
	
	val genLock = new AnyRef()
	var generatedItems = 0
	val maxGenItems = 1000
	val random = new Random()
	val completionsBuffer = new VectorBuilder[QueuedAction[Any]]()
	
	private val log = new TimeLogger(autoflush = true)
	
	def task() = Wait(100.millis)
	
	log.checkPoint(s"Starting tasks. Should take about ${ (5.millis * maxGenItems).description }.")
	
	// Starts generating actions
	while (generatedItems < maxGenItems) {
		completionsBuffer += queue.push { task() }
		generatedItems += 1
		Wait(random.nextInt(10).millis, genLock)
	}
	
	// Checks the completions
	val completions = completionsBuffer.result()
	assert(completions.size == maxGenItems)
	
	log.checkPoint("All completions created")
	private val earlyCompletionCount = completions.count { _.isCompleted }
	println(s"$earlyCompletionCount / $maxGenItems items completed. Should be about ${ maxGenItems / 4 }.")
	
	val successes = completions.flatMap { _.future.waitFor().toOption }
	assert(successes.size == maxGenItems)
	log.checkPoint(s"All completed! The whole process should have taken around ${
		((maxGenItems * 100 / 5).millis + (maxGenItems * 5).millis).description }")
}

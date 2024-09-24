package utopia.flow.test.async

import utopia.flow.async.context.ExcEvent.{QueueCleared, TaskAccepted, TaskCompleted, TaskQueued, ThreadClosed, ThreadCreated}
import utopia.flow.async.context.{ExcEvent, ThreadPool}
import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.async.Volatile

/**
  * Tests [[ThreadPool]]
  * @author Mikko Hilpinen
  * @since 23.09.2024, v2.5
  */
object ThreadPoolTest extends App
{
	// SETUP   --------------------------
	
	private implicit val log: Logger = SysErrLogger
	private implicit val pool: ThreadPool = new ThreadPool("Flow", 2, 5, 6.seconds)
	
	private val eventQueue = Volatile.emptySeq[ExcEvent]
	pool.addListener { e =>
		println(e)
		eventQueue.update { _ :+ e }
	}
	
	private val indexIter = Iterator.iterate(1) { _ + 1 }
	
	
	// TESTS    ------------------------
	
	// Initiates 2 tasks. Should occupy the 2 core threads
	// and cause no new threads to be created (except one for the events)
	pool.execute(TestTask)
	pool.execute(TestTask)
	
	Wait(0.1.seconds)
	testEvents(_.isInstanceOf[TaskAccepted], _.isInstanceOf[ThreadCreated])(2, 1)
	assert(pool.currentSize == 3)
	
	// Waits until the tasks complete.
	// Ensures that events were fired correctly.
	Wait(2.5.seconds)
	testEvents(_.isInstanceOf[TaskCompleted])(2)
	
	// Initiates 4 tasks. Should occupy the 2 core threads, plus 2 temporary threads (plus fifth for events).
	(0 until 4).foreach { _ => pool.execute(TestTask) }
	
	Wait(0.1.seconds)
	testEvents(_.isInstanceOf[TaskAccepted], _.isInstanceOf[ThreadCreated])(4, 2)
	assert(pool.isMaxed)
	
	// Initiates one more task, maxing out the capacity
	// This blocks the thread-pool event-handling
	pool.execute(TestTask)
	
	Wait(0.1.seconds)
	testEvents()()
	assert(pool.isMaxed)
	
	// Initiates one more task, which gets queued
	pool.execute(TestTask)
	
	Wait(0.1.seconds)
	testEvents()()
	assert(pool.isMaxed)
	
	// Waits until the 5 started tasks complete
	// Should fire the 2 tasks accepted -events and the one queued-event
	Wait(2.5.seconds)
	testEvents(_.isInstanceOf[TaskAccepted], _.isInstanceOf[TaskQueued], _.isInstanceOf[TaskCompleted])(2, 1, 5)
	assert(pool.isMaxed)
	
	// Waits for the final task to complete
	Wait(2.5.seconds)
	testEvents(_.isInstanceOf[TaskCompleted], _ == QueueCleared)(1, 1)
	assert(pool.isMaxed)
	
	// Waits for the temporary threads to get disposed
	Wait(8.seconds)
	testEvents(_.isInstanceOf[ThreadClosed])(3)
	assert(pool.currentSize == 2)
	
	println("Success!")
	
	
	// OTHER    ------------------------
	
	private def testEvents(tests: (ExcEvent => Boolean)*)(counts: Int*) = {
		val events = eventQueue.popAll()
		val expectedTestCount = counts.sum
		if (events.size != expectedTestCount)
			throw new IllegalStateException(s"Expected $expectedTestCount events. Got ${ events.size }: [${
				events.mkString(", ") }]")
		else
			tests.zip(counts).foreach { case (test, count) =>
				assert(events.count(test) == count, events.mkString(", "))
			}
	}
	
	
	// NESTED   ------------------------
	
	private object TestTask extends Runnable
	{
		override def run() = {
			val i = indexIter.next()
			println(s"Task $i starting...")
			Wait(2.seconds)
			println(s"Task $i completing")
		}
	}
}

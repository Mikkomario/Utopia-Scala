package utopia.flow.test.async

import utopia.flow.async.process.ProcessState.{Completed, Running}
import utopia.flow.async.process.{TimedTask, TimedTasks, Wait}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.LockablePointer

import java.time.Instant

/**
  * Tests pointer reactions in TimedTasks
  * @author Mikko Hilpinen
  * @since 22.11.2023, v2.3
  */
object ChangingTimedTasksTest extends App
{
	import utopia.flow.test.TestContext._
	
	private def testTask(useLoop: Boolean) = {
		val counter = Volatile(0)
		val nextTimePointer = LockablePointer[Option[Instant]](None)
		val task = TimedTask.immediately.cancellable {
			val newCounterValue = counter.updateAndGet { _ + 1 }
			println(s"Task run #$newCounterValue")
			nextTimePointer.value = Some(Now + 2.seconds)
			nextTimePointer
		}
		val tasks = {
			if (useLoop)
				task.toLoop
			else {
				val tasks = new TimedTasks()
				tasks += task
				tasks
			}
		}
		
		tasks.runAsync()
		Wait(0.5.seconds)
		/*
		if (useLoop) {
			assert(counter.value == 1)
			assert(tasks.state == Completed)
			
			nextTimePointer.value = Some(Now + 1.0.seconds)
			
			Wait(0.5.seconds)
			assert(tasks.state == Running)
			assert(counter.value == 0)
			
			Wait(1.0.seconds)
			assert(tasks.state == Running)
			assert(counter.value == 1)
		}
		else {
			assert(counter.value == 1)
			assert(tasks.state == Running)
		}*/
		
		assert(counter.value == 1)
		assert(tasks.state == Running, tasks.state)
		
		Wait(2.0.seconds)
		assert(counter.value == 2)
		assert(tasks.state == Running)
		
		Wait(2.seconds)
		assert(counter.value == 3)
		assert(tasks.state == Running)
		
		nextTimePointer.value = Some(Now + 0.1.seconds)
		
		Wait(0.5.seconds)
		assert(counter.value == 4)
		assert(tasks.state == Running)
		
		nextTimePointer.value = None
		
		Wait(0.5.seconds)
		tasks.state == Completed
		Wait(1.5.seconds)
		assert(counter.value == 4)
		
		nextTimePointer.value = Some(Now)
		
		Wait(0.5.seconds)
		assert(tasks.state == Running)
		assert(counter.value == 5)
		
		println("\nCancels the next scheduled run (temporary)")
		nextTimePointer.value = None
		println("Locks the scheduled run time pointer, making the cancellation final")
		nextTimePointer.lock()
		println("Scheduled time pointer is now locked")
		assert(nextTimePointer.isFixed)
		
		Wait(0.5.seconds)
		assert(tasks.state.isFinal, tasks.state)
		assert(counter.value == 5)
		Wait(1.5.seconds)
		assert(counter.value == 5)
	}
	
	println("\nPart 1: Testing as loop")
	testTask(useLoop = true)
	
	println("Part 2: Testing as TimedTasks")
	testTask(useLoop = false)
	
	println("Success!")
}

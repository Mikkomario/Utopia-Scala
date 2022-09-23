package utopia.flow.test.time

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.ProcessState.{Cancelled, Completed, NotStarted, Running, Stopped}
import utopia.flow.async.ShutdownReaction.{Cancel, DelayShutdown, SkipDelay}
import utopia.flow.async.process.Wait
import utopia.flow.async.context.{CloseHook, ThreadPool}
import utopia.flow.generic.DataType
import utopia.flow.time.{Now, WaitUtils}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Used for testing Wait
  * @author Mikko Hilpinen
  * @since 25.2.2022, v1.15
  */
object WaitTest2 extends App
{
	DataType.setup()
	implicit val logger: Logger = SysErrLogger
	implicit val exc: ExecutionContext = new ThreadPool("test").executionContext
	
	println("Running WaitTest2...")
	
	// Tests asynchronous waiting
	val lock = new AnyRef
	val w = new Wait(1.seconds, lock)
	
	assert(w.state == NotStarted)
	
	var timeBeforeWait = Now.toInstant
	w.runAsync()
	assert(Now - timeBeforeWait < 0.5.seconds)
	assert(w.completionFuture.waitFor().get == Completed)
	assert(Now - timeBeforeWait > 0.9.seconds)
	
	// Tests synchronous and interrupted waiting
	val lock2 = new AnyRef
	val w2 = new Wait(0.5.seconds, lock2)
	
	timeBeforeWait = Now
	w.runAsync()
	w2.run()
	assert(Now - timeBeforeWait < 0.7.seconds)
	assert(Now - timeBeforeWait > 0.4.seconds)
	assert(w.completionFuture.isEmpty)
	assert(w.state == Running)
	WaitUtils.notify(lock)
	assert(w.completionFuture.waitFor().get == Completed)
	assert(Now - timeBeforeWait < 0.9.seconds)
	
	// Tests cancelled wait
	val w6 = new Wait(10.seconds)
	assert(w6.stop().isCompleted)
	assert(w6.state == Cancelled)
	timeBeforeWait = Now
	w6.run()
	assert(Now - timeBeforeWait < 0.5.seconds)
	assert(w6.state == NotStarted)
	
	// Tests wait stopping
	timeBeforeWait = Now
	w6.runAsync()
	Wait(0.3.seconds)
	assert(w6.state == Running)
	assert(w6.stop().waitFor().get == Stopped)
	assert(Now - timeBeforeWait < 0.5.seconds)
	
	// Tests CloseHook interactions
	val w3 = new Wait(2.seconds, lock, Some(DelayShutdown))
	val w4 = new Wait(5.seconds, lock2, Some(Cancel))
	val w5 = new Wait(10.seconds, shutdownReaction = Some(SkipDelay))
	
	timeBeforeWait = Now
	w3.runAsync()
	w4.runAsync()
	w5.runAsync()
	Wait(0.1.seconds)
	val shutdownFuture = Future { CloseHook.shutdown() }
	Wait(0.1.seconds)
	assert(w3.state == Running)
	assert(w4.state == Stopped)
	assert(w5.state == Completed)
	assert(shutdownFuture.isEmpty)
	shutdownFuture.waitFor()
	assert(Now - timeBeforeWait > 1.9.seconds)
	assert(Now - timeBeforeWait < 4.9.seconds)
	assert(w3.state == Completed)
	
	println("Done!")
}

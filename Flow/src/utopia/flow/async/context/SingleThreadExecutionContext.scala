package utopia.flow.async.context

import utopia.flow.async.process.Breakable
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.Settable
import utopia.flow.view.mutable.async.Volatile
import utopia.flow.view.mutable.eventful.SettableFlag

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * This execution context only uses a single thread, queuing tasks if needed
  * @author Mikko Hilpinen
  * @since 16.4.2023, v2.1
  * @param name The name of this execution context
  * @param keepAliveDuration Duration how long the managed thread should be kept alive in an idle state.
  *                          Default = infinite = The thread will only close once the JVM shuts down or when
  *                          [[stop stop()]] is explicitly called.
  * @param log Implicit logging implementation
  */
class SingleThreadExecutionContext(name: String, keepAliveDuration: Duration = Duration.Inf)(implicit log: Logger)
	extends ExecutionContext with Breakable
{
	// ATTRIBUTES   --------------------------
	
	private val threadPointer = Volatile.empty[WorkerThread]
	
	
	// INITIAL CODE   ------------------------
	
	registerToStopOnceJVMCloses()
	
	
	// IMPLEMENTED  --------------------------
	
	override def execute(runnable: Runnable) = {
		// Creates a new thread, if necessary. Assigns the specified task to the worker thread.
		threadPointer.update { current =>
			val thread = current.filter { _.alive } match {
				// Case: There was a thread ready to receive the new task
				// NB: There's a very slight chance that the thread dies between these two lines of code...
				case Some(thread) =>
					thread.assign(runnable)
					thread
				// Case: There was no thread ready => Creates a new one
				case None =>
					val thread = new WorkerThread
					thread.assign(runnable)
					thread.start()
					thread
			}
			Some(thread)
		}
	}
	override def reportFailure(cause: Throwable) = log(cause)
	
	override def stop(): Future[Any] = threadPointer.pop() match {
		case Some(thread) => thread.end()
		case None => Future.successful(())
	}
	
	
	// NESTED   ------------------------------
	
	private class WorkerThread extends Thread
	{
		// ATTRIBUTES   ----------------------
		
		private val waitLock = new AnyRef
		private val killedFlag = Settable()
		private val finishedFlag = SettableFlag()
		private val taskQueue = Volatile.eventful.seq[Runnable]()
		
		
		// INITIAL CODE ----------------------
		
		setName(name)
		setDaemon(true)
		
		
		// COMPUTED --------------------------
		
		def alive = killedFlag.isNotSet
		
		
		// IMPLEMENTED  ----------------------
		
		override def run() = {
			var lastTaskTime = Now.toInstant
			while (killedFlag.isNotSet) {
				taskQueue.pop() match {
					case Some(task) =>
						task.run()
						lastTaskTime = Now
					case None =>
						val expiration = keepAliveDuration.finite.map { lastTaskTime + _ }
						val maxWaitMillis = expiration match {
							case Some(expiration) => (expiration - Now).toMillis + 1
							case None => 0L
						}
						waitLock.synchronized {
							if (taskQueue.isEmpty) {
								// Schedules a break in the wait once there are tasks available
								taskQueue.once { _.nonEmpty } { _ => waitLock.notifyAll() }
								
								// Waits until notified, or until max idle time is reached
								try {
									waitLock.wait(maxWaitMillis)
									// Checks whether this thread should expire
									if (taskQueue.isEmpty && expiration.exists { _ <= Now })
										killedFlag.set()
								}
								catch {
									// Case: Interrupted => Kills this thread
									case e: InterruptedException =>
										log(e)
										killedFlag.set()
								}
							}
						}
				}
			}
			finishedFlag.set()
			threadPointer.filterNotCurrent { _ == this }
		}
		
		
		// OTHER    -------------------------
		
		// Synchronizes in order to avoid a situation where a task is assigned before the wait call
		def assign(task: Runnable) = waitLock.synchronized { taskQueue :+= task }
		
		def end() = {
			killedFlag.set()
			waitLock.synchronized { waitLock.notifyAll() }
			finishedFlag.future
		}
	}
}

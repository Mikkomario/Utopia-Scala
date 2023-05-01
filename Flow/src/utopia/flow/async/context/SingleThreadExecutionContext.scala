package utopia.flow.async.context

import utopia.flow.async.process.Breakable
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.Flag

import scala.concurrent.{ExecutionContext, Future}

/**
  * This execution context only uses a single thread, queuing tasks if needed
  * @author Mikko Hilpinen
  * @since 16.4.2023, v2.1
  * @param name The name of this execution context
  * @param log Implicit logging implementation
  */
class SingleThreadExecutionContext(name: String)(implicit log: Logger)
	extends ExecutionContext
{
	// INITIAL CODE   ------------------------
	
	Breaker.registerToStopOnceJVMCloses()
	WorkerThread.start()
	
	
	// IMPLEMENTED  --------------------------
	
	override def execute(runnable: Runnable) = WorkerThread.assign(runnable)
	override def reportFailure(cause: Throwable) = log(cause)
	
	
	// NESTED   ------------------------------
	
	private object Breaker extends Breakable
	{
		override def stop(): Future[Any] = WorkerThread.end()
	}
	
	private object WorkerThread extends Thread
	{
		// ATTRIBUTES   ----------------------
		
		private val waitLock = new AnyRef
		private val killedFlag = Flag()
		private val finishedFlag = Flag()
		private val taskQueue = VolatileList[Runnable]()
		
		
		// INITIAL CODE ----------------------
		
		setName(name)
		setDaemon(true)
		
		
		// IMPLEMENTED  ----------------------
		
		override def run() = {
			while (killedFlag.isNotSet) {
				taskQueue.pop() match {
					case Some(task) => task.run()
					case None =>
						waitLock.synchronized {
							taskQueue.once { _.nonEmpty } { _ => waitLock.synchronized { waitLock.notifyAll() } }
							try { waitLock.wait() }
							catch {
								case e: InterruptedException =>
									log(e)
									killedFlag.set()
							}
						}
				}
			}
			finishedFlag.set()
		}
		
		
		// OTHER    -------------------------
		
		def assign(task: Runnable) = taskQueue :+= task
		
		def end() = {
			killedFlag.set()
			waitLock.synchronized { waitLock.notifyAll() }
			finishedFlag.future
		}
	}
}

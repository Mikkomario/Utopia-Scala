package utopia.flow.async.context

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Common trait for events fired by an execution context (in Flow, that is [[ThreadPool]])
  * @author Mikko Hilpinen
  * @since 23.09.2024, v2.5
  */
sealed trait ExcEvent

object ExcEvent
{
	// NESTED   ----------------------------
	
	sealed trait ThreadEvent extends ExcEvent
	{
		/**
		  * @return Name of the thread where this event occurred
		  */
		def name: String
	}
	
	/**
	  * An event fired when a new (temporary) worker thread is created
	  * @param name Name of the new thread
	  * @param numberOfThreads The current number of threads, including this newly created thread
	  */
	case class ThreadCreated(name: String, numberOfThreads: Int) extends ThreadEvent
	/**
	  * An event fired when a temporary worker thread is closed / ends
	  * @param name Name of this thread
	  * @param remainingNumberOfThreads Number of threads remaining in the pool after the closing of this thread
	  */
	case class ThreadClosed(name: String, remainingNumberOfThreads: Int) extends ThreadEvent
	
	/**
	  * An event fired when a worker thread accepts a new task
	  * @param name Name of the thread that accepted the task
	  * @param idleDuration Duration how long this thread was idle before receiving this task
	  * @param queueDuration Duration how long the task was queued (i.e. blocked) before passed to this thread
	  */
	case class TaskAccepted(name: String, idleDuration: FiniteDuration = Duration.Zero,
	                        queueDuration: FiniteDuration = Duration.Zero)
		extends ThreadEvent
	/**
	  * An event fired when a worker thread completes a task
	  * @param name Name of the thread that finished the task
	  * @param taskDuration Duration of the task's execution
	  */
	case class TaskCompleted(name: String, taskDuration: FiniteDuration) extends ThreadEvent
	
	/**
	  * An event fired when a task becomes temporarily queued, meaning that it's not accepted by any thread.
	  * This only occurs when the thread pool's whole maximum capacity is in use
	  * @param queueSize Number of tasks currently queued
	  */
	case class TaskQueued(queueSize: Int) extends ExcEvent
	/**
	  * An event fired when the task queue becomes empty,
	  * meaning that the thread pool again has capacity to immediately run new tasks.
	  */
	case object QueueCleared extends ExcEvent
}
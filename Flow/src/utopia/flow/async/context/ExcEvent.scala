package utopia.flow.async.context

import utopia.flow.time.TimeExtensions._

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
	{
		override def toString = s"Thread $name created"
	}
	/**
	  * An event fired when a temporary worker thread is closed / ends
	  * @param name Name of this thread
	  * @param remainingNumberOfThreads Number of threads remaining in the pool after the closing of this thread
	  */
	case class ThreadClosed(name: String, remainingNumberOfThreads: Int) extends ThreadEvent
	{
		override def toString = s"Thread $name closed"
	}
	
	/**
	  * An event fired when a worker thread accepts a new task
	  * @param name Name of the thread that accepted the task
	  * @param idleDuration Duration how long this thread was idle before receiving this task
	  * @param queueDuration Duration how long the task was queued (i.e. blocked) before passed to this thread
	  */
	case class TaskAccepted(name: String, idleDuration: FiniteDuration = Duration.Zero,
	                        queueDuration: FiniteDuration = Duration.Zero)
		extends ThreadEvent
	{
		override def toString = {
			if (queueDuration > Duration.Zero)
				s"Thread $name accepted a task that was queued for ${ queueDuration.description }"
			else if (idleDuration > Duration.Zero)
				s"Thread $name accepted a task after waiting for ${ idleDuration.description }"
			else
				s"Thread $name accepted the next task"
		}
	}
	/**
	  * An event fired when a worker thread completes a task
	  * @param name Name of the thread that finished the task
	  * @param taskDuration Duration of the task's execution
	  */
	case class TaskCompleted(name: String, taskDuration: FiniteDuration) extends ThreadEvent
	{
		override def toString = s"Thread $name completed a task in ${ taskDuration.description }"
	}
	
	/**
	  * An event fired when a task becomes temporarily queued, meaning that it's not accepted by any thread.
	  * This only occurs when the thread pool's whole maximum capacity is in use
	  * @param queueSize Number of tasks currently queued
	  */
	case class TaskQueued(queueSize: Int) extends ExcEvent
	{
		override def toString = s"A task was queued at position $queueSize"
	}
	/**
	  * An event fired when the task queue becomes empty,
	  * meaning that the thread pool again has capacity to immediately run new tasks.
	  */
	case object QueueCleared extends ExcEvent
	{
		override def toString = "Task queue was cleared"
	}
}
package utopia.flow.async.context

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ExcEvent.{QueueCleared, TaskAccepted, TaskCompleted, TaskQueued, ThreadClosed, ThreadCreated}
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.NotEmpty
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.Volatile

import java.time.Instant
import java.util.concurrent.Executor
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Promise}
import scala.util.Try

/**
* This class handles thread reuse and task distribution
* @author Mikko Hilpinen
* @since 28.3.2019
**/
class ThreadPool(val name: String, coreSize: Int = 5, val maxSize: Int = 250,
                 val maxIdleDuration: FiniteDuration = 1.minutes)
                (implicit log: Logger)
    extends Executor with ExecutionContext
{
    // ATTRIBUTES    ---------------------
    
    private val indexCounter = Iterator.iterate(1) { _ + 1 }
	// Creates the core threads from the very beginning
	private val threads: Volatile[Seq[WorkerThread2]] = Volatile.seq(
		(0 until coreSize).map { i => WorkerThread2.core(s"$name-core-${ i + 1 }", i) })
	// Contains tasks which could not be immediately given to a worker thread
	// Each entry contains:
	//      1. The queued task
	//      2. Whether events should be generated for this task (false for event-handling tasks)
	//      3. Time when this task was added to the queue
    private val queue = Volatile.seq[(Runnable, Boolean, Instant)]()
	
	private var listeners: Seq[ExcListener] = Empty
	// Contains generated execution context -events.
	// These are fired asynchronously at the earliest opportunity.
	private val eventQueue = Volatile.emptySeq[ExcEvent]
    
    
    // IMPLEMENTED    --------------------
    
    override def reportFailure(cause: Throwable) = log(cause)
    
    /**
	 * Executes a task asynchronously.
     * If maximum amount of simultaneous tasks has been reached,
	 * the execution of the task will wait until some of the previous tasks have been handled
	 */
	def execute(task: Runnable) = _execute(task)
	
	
	// OTHER    --------------------------
	
	/**
	  * Adds a new listener to be informed of execution context -events
	  * @param listener Listener to inform of new events
	  */
	def addListener(listener: ExcListener) = listeners :+= listener
	/**
	  * Removes a listener from being informed about execution context -events
	  * @param listener Listener to no longer be informed about events
	  */
	def removeListener(listener: Any) = listeners = listeners.filterNot { _ == listener }
	
    private def nextThreadName = s"$name-${indexCounter.next()}"
	
    private def nextQueueTask = {
	    // Unqueues a single task
	    val (unqueued, wasCleared) = queue.mutate { queue =>
		    queue.headOption match {
			    case Some(nextTask) =>
				    val remaining = queue.tail
				    (Some(nextTask) -> remaining.isEmpty) -> remaining
				    
			    case None => (None -> false) -> Empty
		    }
	    }
	    // May fire an event, also
	    if (wasCleared)
		    fireEvent(QueueCleared)
	    
	    unqueued
    }
	
	private def _execute(task: Runnable, eventful: Boolean = true): Unit = {
		val wasQueued = threads.mutate { current =>
			// First checks if any of the existing threads accepts the task
			if (current.exists { _.offer(task, eventful) })
				false -> current
			else {
				// If all were busy, tries to create a new thread
				val currentThreadCount = current.size
				if (currentThreadCount < maxSize) {
					val newThread = WorkerThread2.temp(nextThreadName, currentThreadCount, maxIdleDuration, task,
						eventful)
					false -> (current :+ newThread)
				}
				else {
					// If max thread limit is reached, pushes the task to queue
					queue :+= (task, eventful, Now)
					true -> current
				}
			}
		}
		// If an event gets queued, may generate an event
		if (wasQueued && eventful)
			fireEvent(TaskQueued(queue.size), synchronous = true)
	}
	
	private def fireEvent(event: => ExcEvent, synchronous: Boolean = false) = {
		// Won't bother to perform event-handling if there are no listeners attached
		if (listeners.nonEmpty) {
			// Case: Event-firing is synchronous, unless queued already
			if (synchronous) {
				// May extend the queue, but doesn't start a new one
				val wasQueued = eventQueue.mutate { queue =>
					if (queue.isEmpty)
						false -> Empty
					else
						true -> (queue :+ event)
				}
				// If not queued, immediately fires the event synchronously
				if (!wasQueued) {
					val e = event
					listeners.foreach { l =>
						Try { l.onExcEvent(e) }.logWithMessage("An exc event listener threw an exception")
					}
				}
			}
			// Case: Asynchronous event-handling
			else {
				// Queues the event
				val shouldClearQueue = eventQueue.mutate { queued => queued.isEmpty -> (queued :+ event) }
				// If not running already, initializes the event queue clearance protocol (async)
				if (shouldClearQueue)
					_execute(ClearEventQueueTask, eventful = false)
			}
		}
	}
	
	
	// NESTED   --------------------------
	
	private object ClearEventQueueTask extends Runnable
	{
		override def run() = {
			OptionsIterator.continually { NotEmpty(eventQueue.popAll()) }.foreach { events =>
				events.foreach { event =>
					listeners.foreach { l =>
						Try { l.onExcEvent(event) }.logWithMessage("An exc event listener threw an exception")
					}
				}
			}
		}
	}
	
	private object WorkerThread2
	{
		def core(name: String, existingThreadCount: Int): WorkerThread2 =
			apply(existingThreadCount) { new WorkerThread2(name) }
		
		def temp(name: String, existingThreadCount: Int, maxIdleDuration: Duration, initialTask: Runnable,
		         eventful: Boolean = true): WorkerThread2 =
			apply(existingThreadCount, eventful) {
				new WorkerThread2(name, maxIdleDuration, Some(initialTask -> eventful))
			}
		
		private def apply(existingThreadCount: Int, eventful: Boolean = true)
		                 (construct: => WorkerThread2): WorkerThread2 =
		{
			// Creates and starts the thread
			val thread = construct
			thread.start()
			
			// Fires an event, if appropriate
			if (eventful)
				fireEvent(ThreadCreated(thread.name, existingThreadCount + 1))
			
			thread
		}
	}
	private class WorkerThread2(val name: String, maxIdleDuration: Duration = Duration.Inf,
	                            initialTask: Option[(Runnable, Boolean)] = None)
		extends Thread
	{
		// ATTRIBUTES    ---------------------
		
		private val ended = Volatile.switch
		// Some(...) when accepting a new task, None when not accepting
		// Contains 2 values when waiting for a task:
		//      1. A promise for holding the task (+ whether events should be generated)
		//      2. Time when this promise was initiated / wait was started
		private val waitingTask = Volatile.optional[(Promise[(Runnable, Boolean)], Instant)]()
		
		private var nextTask = initialTask
		
		
		// INITIAL CODE    -------------------
		
		setName(name)
		setDaemon(true)
		
		
		// COMPUTED    -----------------------
		
		private def isEnded = ended.isSet
		
		
		// IMPLEMENTED    --------------------
		
		override def run() = {
			while (ended.isNotSet) {
				// Finds the next task to perform, may fail if maximum idle duration is reached
				val next = nextTask.orElse {
					// Starts waiting for the next task
					val nextFuture = waitingTask
						.mutate {
							case Some((existing, initialized)) => existing -> Some(existing -> initialized)
							case None =>
								val promise = Promise[(Runnable, Boolean)]()
								promise -> Some(promise -> Now)
						}
						.future
					
					nextFuture.waitFor(maxIdleDuration).toOption
				}
				
				// If no task was found, ends
				next match {
					// Case: Next task is available => Performs it (catches errors)
					case Some((next, eventful)) =>
						val taskStartTime = Now.toInstant
						Try { next.run() }.logWithMessage(s"Exception reached thread $name")
						if (eventful)
							fireEvent(TaskCompleted(name, Now - taskStartTime))
						
						// Takes the next task right away, if one is available
						nextQueueTask.foreach { case (task, eventful, queued) =>
							nextTask = Some(task -> eventful)
							if (eventful)
								fireEvent(TaskAccepted(name, queueDuration = Now - queued))
						}
					
					// Case: No task available => Ends
					case None => ended.set()
				}
			}
			
			// Clears remaining task references
			nextTask = None
			waitingTask.clear()
			
			// Clears this thread from the thread pool
			val remainingThreadCount = threads.updateAndGet { _.filterNot { _.isEnded } }.size
			// If appropriate, fires a thread cleared -event
			fireEvent(ThreadClosed(name, remainingThreadCount), synchronous = true)
		}
		
		
		// OTHER    -------------------------
		
		/**
		  * Offers a new task for this thread. This thread will accept the task if it's not busy already.
		  * @param task the task to be performed
		  * @return whether this thread accepted the task
		  */
		def offer(task: Runnable, eventful: Boolean = true) = {
			// Only accepts new tasks if not busy already
			if (ended.isNotSet) {
				waitingTask.lockWhile { waiter =>
					// Proposes the task to a waiting promise,
					// if there is one and if that promise hasn't been completed already
					waiter.filter { _._1.trySuccess(task -> eventful) } match {
						// Case: Task was accepted => Fires an event, also, if appropriate
						case Some((_, waitStarted)) =>
							fireEvent(TaskAccepted(name, Now - waitStarted))
							true
							
						// Case: Not waiting for a task => Rejects the task
						case None => false
					}
				}
			}
			// Case: Already ended => Rejects the task
			else
				false
		}
	}
}




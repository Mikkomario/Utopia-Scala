package utopia.flow.async.context

import utopia.flow.async.AsyncExtensions._
import utopia.flow.async.context.ExcEvent.{QueueCleared, TaskAccepted, TaskCompleted, TaskQueued, ThreadClosed, ThreadCreated}
import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.NotEmpty
import utopia.flow.util.TryExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.Pointer
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
	
	private var listeners: Seq[ExcListener] = Empty
	// Contains generated execution context -events.
	// These are fired asynchronously at the earliest opportunity.
	// The second value is a boolean indicating whether the event-delivery process is currently active
	private val eventQueue = Volatile[(Seq[ExcEvent], Boolean)](Empty -> false)
	
    private val indexCounter = Iterator.iterate(coreSize + 1) { _ + 1 }
	// Creates the core threads from the very beginning
	private val threads: Volatile[Seq[WorkerThread2]] = Volatile.seq(
		(0 until coreSize).map { i =>
			val thread = WorkerThread2.core(s"$name-core-${ i + 1 }", i)._1
			thread.start()
			thread
		})
	// Contains tasks which could not be immediately given to a worker thread
	// Each entry contains:
	//      1. The queued task
	//      2. Whether events should be generated for this task (false for event-handling tasks)
	//      3. Time when this task was added to the queue
    private val queue = Volatile.seq[(Runnable, Boolean, Instant)]()
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The current number of reserved threads
	  */
	def currentSize = threads.value.size
	/**
	  * @return The current number of active threads
	  *         (performing some task, although the task itself may be in a waiting state)
	  */
	def currentActiveSize = threads.value.count { _.running }
	/**
	  * @return Current number of queued (i.e. pending) tasks
	  */
	def currentQueueSize = queue.value.size
	
	/**
	  * @return Whether this thread-pool is currently operating at maximum capacity,
	  *         i.e. whether maximum number of threads have been created and each is in use.
	  */
	def isMaxed = {
		if (queue.value.nonEmpty)
			true
		else {
			val t = threads.value
			t.hasSize(maxSize) && t.forall { _.running }
		}
	}
	
	/**
	  * @return State of each thread in this pool.
	  *         Contains one entry for each reserved thread. Each entry contains 2 values:
	  *             1. Whether this thread is currently performing a task (true) or waiting to receive one (false)
	  *             1. Since when has this state been active
	  */
	def threadStates = threads.value.map { _.state }
    
    
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
		    fireEvent(QueueCleared, synchronous = true)
	    
	    unqueued
    }
	
	private def _execute(task: Runnable, eventful: Boolean = true): Unit = {
		val (newThread, events) = threads.mutate { current =>
			// First checks if any of the existing threads accepts the task
			if (current.exists { _.offer(task, eventful) })
				(None -> Empty) -> current
			else {
				// If all were busy, tries to create a new thread
				val currentThreadCount = current.size
				if (currentThreadCount < maxSize) {
					val (newThread, events) = WorkerThread2.temp(nextThreadName, currentThreadCount, maxIdleDuration,
						task, eventful)
					(Some(newThread), events) -> (current :+ newThread)
				}
				else {
					// If max thread limit is reached, pushes the task to queue
					val queueSize = queue.updateAndGet { _ :+ (task, eventful, Now) }.size
					val events = if (eventful) Single(TaskQueued(queueSize)) else Empty
					(None -> events) -> current
				}
			}
		}
		// Fires the events, if appropriate
		fireEvents(events)
		// Starts the new thread, if applicable
		newThread.foreach { _.start() }
	}
	
	private def fireEvent(event: => ExcEvent, synchronous: Boolean = false) =
		fireEvents(Single(event), synchronous)
	
	private def fireEvents(events: => Seq[ExcEvent], synchronous: Boolean = false) = {
		// Won't bother to perform event-handling if there are no listeners attached
		if (listeners.nonEmpty) {
			NotEmpty(events).foreach { events =>
				// Case: Event-firing is synchronous, unless queued already
				if (synchronous) {
					// May extend the queue, but doesn't start a new one
					val wasDelivering = eventQueue.mutate { case (queue, delivering) =>
						if (delivering)
							true -> ((queue ++ events) -> true)
						else
							false -> (queue -> delivering)
					}
					// If not delivering events asynchronously, immediately fires the event synchronously
					if (!wasDelivering)
						events.foreach { event =>
							listeners.foreach { l =>
								Try { l.onExcEvent(event) }.logWithMessage("An exc event listener threw an exception")
							}
						}
				}
				// Case: Asynchronous event-handling
				else {
					// Queues the event
					val wasDelivering = eventQueue.mutate { case (queued, delivering) =>
						delivering -> ((queued ++ events) -> true)
					}
					// If not running already, initializes the event queue clearance protocol (async)
					if (!wasDelivering)
						_execute(ClearEventQueueTask, eventful = false)
				}
			}
		}
	}
	
	
	// NESTED   --------------------------
	
	private object ClearEventQueueTask extends Runnable
	{
		override def run() = {
			OptionsIterator
				.continually {
					val nextEvents = eventQueue
						.mutate { case (queue, _) => queue -> (Empty -> queue.nonEmpty) }
					NotEmpty(nextEvents)
				}
				.foreach { events =>
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
		def core(name: String, existingThreadCount: Int): (WorkerThread2, Seq[ExcEvent]) =
			apply(name, existingThreadCount)
		
		def temp(name: String, existingThreadCount: Int, maxIdleDuration: Duration, initialTask: Runnable,
		         eventful: Boolean = true): (WorkerThread2, Seq[ExcEvent]) =
			apply(name, existingThreadCount, maxIdleDuration, Some(initialTask -> eventful))
		
		private def apply(name: String, existingThreadCount: Int, maxIdleDuration: Duration = Duration.Inf,
		                  initialTask: Option[(Runnable, Boolean)] = None): (WorkerThread2, Seq[ExcEvent]) =
		{
			// Creates the thread
			val thread = new WorkerThread2(name, maxIdleDuration, initialTask)
			
			// Prepares events
			val events = Single(ThreadCreated(name, existingThreadCount + 1)) ++
				initialTask.filter { _._2 }.map { _ => TaskAccepted(name) }
			
			// Returns the thread and the events
			thread -> events
		}
	}
	private class WorkerThread2(name: String, maxIdleDuration: Duration = Duration.Inf,
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
		
		private var _lastTaskStartTime = Now.toInstant
		
		
		// INITIAL CODE    -------------------
		
		setName(name)
		setDaemon(true)
		
		
		// COMPUTED    -----------------------
		
		def running = waitingTask.isEmpty && ended.isNotSet
		
		// Returns:
		//      1. Whether running a task (true) or waiting (false)
		//      2. Since when
		def state = waitingTask.value match {
			case Some((_, since)) => false -> since
			case None => true -> _lastTaskStartTime
		}
		
		private def isEnded = ended.isSet
		
		
		// IMPLEMENTED    --------------------
		
		override def run() = {
			val nextTaskPointer = Pointer(initialTask)
			while (ended.isNotSet) {
				// Finds the next task to perform, may fail if maximum idle duration is reached
				val next = nextTaskPointer.pop().orElse {
					// Starts waiting for the next task
					val nextFuture = waitingTask
						.mutate { existing =>
							existing.filterNot { _._1.isCompleted } match {
								// Case: Still using an incomplete waiter (not typical)
								case Some((existing, initialized)) => existing -> Some(existing -> initialized)
								// Case: Needs a new waiting promise
								case None =>
									val promise = Promise[(Runnable, Boolean)]()
									promise -> Some(promise -> Now)
							}
						}
						.future
					
					nextFuture.waitFor(maxIdleDuration).toOption
				}
				
				// If no task was found, ends
				next match {
					// Case: Next task is available => Performs it (catches errors)
					case Some((next, eventful)) =>
						_lastTaskStartTime = Now.toInstant
						Try { next.run() }.logWithMessage(s"Exception reached thread $name")
						if (eventful)
							fireEvent(TaskCompleted(name, Now - _lastTaskStartTime))
						
						// Takes the next task right away, if one is available
						nextQueueTask.foreach { case (task, eventful, queued) =>
							nextTaskPointer.value = Some(task -> eventful)
							if (eventful)
								fireEvent(TaskAccepted(name, queueDuration = Now - queued))
						}
					
					// Case: No task available => Ends
					case None => ended.set()
				}
			}
			
			// Clears remaining task references
			nextTaskPointer.clear()
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
							if (eventful)
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




package utopia.flow.async.context

import utopia.flow.async.AsyncExtensions._
import utopia.flow.collection.mutable.VolatileList
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.async.{VolatileFlag, VolatileOption}

import java.util.concurrent.{Executor, TimeUnit}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Promise}
import scala.util.Try

/**
* This class handles thread reuse and distribution
* @author Mikko Hilpinen
* @since 28.3.2019
**/
class ThreadPool(val name: String, coreSize: Int = 5, val maxSize: Int = 250,
                 val maxIdleDuration: FiniteDuration = Duration(1, TimeUnit.MINUTES))
                (implicit logger: Logger)
    extends Executor with ExecutionContext
{
    // ATTRIBUTES    ---------------------
    
    private val indexCounter = Iterator.iterate(1) { _ + 1 }
    // Creates the core threads from the very beginning
    private val threads = VolatileList(Vector.fill(coreSize)(WorkerThread.core(nextCoreName()) { nextQueueTask() }))
    private val queue = VolatileList[Runnable]()
    
    /**
     * An execution context based on this thread pool
     */
    @deprecated("Please use this ThreadPool instance instead", "< v2.3")
    lazy implicit val executionContext: ExecutionContext = this
    
    
    // IMPLEMENTED    --------------------
    
    override def reportFailure(cause: Throwable) = logger(cause)
    
    /**
	 * Executes a task asynchronously. If maximum amount of simultaneous tasks has been reached, 
	 * the execution of the task will wait until some of the previous tasks have been handled
	 */
	def execute(task: Runnable) = {
        threads.update { current =>
            val filtered = current.filterNot { _.isEnded }
            
            // First checks if any of the existing threads accepts the task
            if (filtered.exists { _ offer task })
                filtered
            else {
                // If all were busy, tries to create a new thread
                if (filtered.hasSize < maxSize)
                    filtered :+ WorkerThread.temp(nextThreadName(), maxIdleDuration, task) { nextQueueTask() }
                else {
                    // If max thread limit is reached, pushes the task to queue
                    queue :+= task
                    filtered
                }
            }
        }
	}
	
	
	// OTHER    --------------------------
	
	private def nextCoreName() = s"$name-core-${indexCounter.next()}"
    
    private def nextThreadName() = s"$name-${indexCounter.next()}"
    
    private def nextQueueTask() = queue.pop()
}

private object WorkerThread
{
    def core(name: String)(getWaitingTask: => Option[Runnable])(implicit logger: Logger) =
    {
        val t = new WorkerThread(name, Duration.Inf, None, getWaitingTask)
        t.start()
        t
    }
    
    def temp(name: String, maxIdleDuration: Duration, initialTask: Runnable)(getWaitingTask: => Option[Runnable])
            (implicit logger: Logger) =
    {
        val t = new WorkerThread(name, maxIdleDuration, Some(initialTask), getWaitingTask)
        t.start()
        t
    }
}

private class WorkerThread(name: String, val maxIdleDuration: Duration, initialTask: Option[Runnable],
                           getWaitingTask: => Option[Runnable])(implicit logger: Logger)
    extends Thread
{
    // ATTRIBUTES    ---------------------
    
    private val ended = new VolatileFlag()
    // Some(...) when accepting a new task, None when not accepting
    private val waitingTask = VolatileOption[Promise[Runnable]]()
    
    private var nextTask = initialTask
    
    
    // INITIAL CODE    -------------------
    
    setName(name)
    setDaemon(true)
    
    
    // COMPUTED    -----------------------
    
    def isEnded = ended.isSet
    
    
    // IMPLEMENTED    --------------------
    
    override def run() = {
        while (ended.isNotSet) {
            // Finds the next task to perform, may fail if maximum idle duration is reached
            val next = nextTask.orElse {
                // Starts waiting for the next task
                val nextFuture = waitingTask.setOne(Promise()).future
                nextFuture.waitFor(maxIdleDuration).toOption
            }
            
            // If no task was found, ends
            next match {
                // Case: Next task is available => Performs it (caches errors)
                case Some(next) =>
                    Try { next.run() }.failure.foreach { logger(_, s"Exception reached thread $name") }
                    // Takes the next task right away, if one is available
                    nextTask = getWaitingTask
                // Case: No task available => Ends
                case None => ended.set()
            }
        }
        waitingTask.clear()
    }
    
    
    // OTHER    -------------------------
    
    /**
     * Offers a new task for this thread. This thread will accept the task if it's not busy already
     * @param task the task to be performed
     * @return whether this thread accepted the task
     */
    def offer(task: Runnable) = {
        // Only accepts new tasks if not busy already
        if (ended.isNotSet) {
            waitingTask.lock { opt =>
                opt.filterNot { _.isCompleted } match {
                    // Case: Waiting for a task => Provides a task
                    case Some(waitingThread) =>
                        waitingThread.success(task)
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
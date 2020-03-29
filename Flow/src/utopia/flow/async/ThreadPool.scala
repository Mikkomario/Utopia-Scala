package utopia.flow.async

import utopia.flow.async.AsyncExtensions._
import utopia.flow.util.CollectionExtensions._
import java.util.concurrent.{Executor, TimeUnit}

import utopia.flow.collection.VolatileList
import utopia.flow.util.Counter

import scala.concurrent.Promise
import scala.concurrent.duration.Duration
import scala.concurrent.duration.FiniteDuration
import scala.util.Try
import scala.concurrent.ExecutionContext

/**
* This class handles thread reuse and distribution
* @author Mikko Hilpinen
* @since 28.3.2019
**/
class ThreadPool(val name: String, coreSize: Int = 5, val maxSize: Int = 250, val maxIdleDuration: FiniteDuration = Duration(1, TimeUnit.MINUTES),
                 val errorHandler: Throwable => Unit = e => e.printStackTrace()) extends Executor
{
    // ATTRIBUTES    ---------------------
    
    private val indexCounter = new Counter(1)
    // Creates the core threads from the very beginning
    private val threads = VolatileList(Vector.fill(coreSize)(WorkerThread.core(nextCoreName(), errorHandler, () => nextQueueTask())))
    private val queue = VolatileList[Runnable]()
    
    /**
     * An execution context based on this thread pool
     */
    lazy val executionContext = ExecutionContext.fromExecutor(this)
    
    
    // IMPLEMENTED    --------------------
    
    /**
	 * Executes a task asynchronously. If maximum amount of simultaneous tasks has been reached, 
	 * the execution of the task will wait until some of the previous tasks have been handled
	 */
	def execute(task: Runnable) = 
	{
        threads.update
        {
            current => 
                
                val filtered = current.filterNot { _.isEnded }
                
                // First checks if any of the existing threads accepts the task
                if (filtered.exists { _ offer task })
                    filtered
                else
                {
                    // If all were busy, tries to create a new thread
                    if (filtered.size < maxSize)
                        filtered :+ WorkerThread.temp(nextThreadName(), maxIdleDuration, task, errorHandler, () => nextQueueTask())
                    else
                    {
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
    def core(name: String, errorHandler: Throwable => Unit, getWaitingTask: () => Option[Runnable]) = 
    {
        val t = new WorkerThread(name, Duration.Inf, None, errorHandler, getWaitingTask)
        t.start()
        t
    }
    
    def temp(name: String, maxIdleDuration: Duration, initialTask: Runnable, 
            errorHandler: Throwable => Unit, getWaitingTask: () => Option[Runnable]) = 
    {
        val t = new WorkerThread(name, maxIdleDuration, Some(initialTask), errorHandler, getWaitingTask)
        t.start()
        t
    }
}

private class WorkerThread(name: String, val maxIdleDuration: Duration, initialTask: Option[Runnable], 
        val errorHandler: Throwable => Unit, val getWaitingTask: () => Option[Runnable]) extends Thread
{
    // ATTRIBUTES    ---------------------
    
    private val ended = new VolatileFlag()
    private val waitingTask = VolatileOption[Promise[Runnable]]()
    
    private var nextTask = initialTask
    
    
    // INITIAL CODE    -------------------
    
    setName(name)
    setDaemon(true)
    
    
    // COMPUTED    -----------------------
    
    def isEnded = ended.isSet
    
    
    // IMPLEMENTED    --------------------
    
    override def run() = 
    {
        while (!ended.isSet)
        {
            // Finds the next task to perform, may fail if maximum idle duration is reached
            val next = nextTask orElse 
            {
                val nextFuture = waitingTask.setOneAndGet(Promise()).future
                nextFuture.waitFor(maxIdleDuration).toOption
            }
            
            // If no task was found, ends
            if (next.isEmpty)
                ended.set()
            else
            {
                // Otherwise performs the task (caches errors)
                Try(next.get.run()).failure.foreach(errorHandler)
                
                // Takes the next task right away, if one is available
                nextTask = getWaitingTask()
            }
        }
        
        // TODO: Clear memory?
    }
    
    
    // OTHER    -------------------------
    
    /**
     * Offers a new task for this thread. This thread will accept the task if it's not busy already
     * @param task the task to be performed
     * @return whether this thread accepted the task
     */
    def offer(task: Runnable) = 
    {
        // Only accepts new tasks if not busy already
        if (!ended.isSet)
        {
            // If this thread is waiting for another task, provides it
            waitingTask.lock
            {
                opt =>
                    if (opt.exists { !_.isCompleted })
                    {
                        opt.get.success(task)
                        true
                    }
                    else
                        false
            }
        }
        else
            false
    }
}
package utopia.flow.async

import utopia.flow.time.WaitUtils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
* This object provides utility tools for asynchronous processes
* @author Mikko Hilpinen
* @since 18.11.2018
**/
object Async
{
    /**
     * Creates a repeating background process. The process will always be performed at least once
     * @param interval the time interval between repeats
     * @param checkContinue a check that will be performed before continuing the process. If 
     * the check returns false, the repeat is terminated
     * @param operation the operation that will be performed
     * @return a future for the end of the repeat process
     */
	def repeat(interval: Duration, checkContinue: () => Boolean, operation: () => Unit)(implicit ec: ExecutionContext) = Future
	{
	    val lock = new AnyRef()
	    do
	    {
	        operation()
	        WaitUtils.wait(interval, lock)
	    }
	    while (checkContinue())
	}
	
	/**
	 * Repeats a background process forever
	 * @param interval the repeat interval
	 * @param operation the operation that will be repeated
	 */
	def repeatForever(interval: Duration, operation: () => Unit)(implicit ec: ExecutionContext) = 
	{
	    Future
    	{
    	    val lock = new AnyRef()
    	    while (true)
    	    {
    	        operation()
    	        WaitUtils.wait(interval, lock)
    	    }
    	}
	    ()
	}
	
	/**
	 * Performs an operation after a delay
	 * @param delay the delay before starting the operation
	 * @param operation the operation that will be performed
	 * @return A future of the operation's result
	 */
	def delayed[T](delay: Duration, operation: () => T)(implicit ec: ExecutionContext) = Future
	{
	    WaitUtils.wait(delay, new AnyRef())
	    operation()
	}
}
package utopia.flow.async.process

import utopia.flow.async.AsyncExtensions._
import utopia.flow.view.mutable.async.VolatileFlag

import scala.concurrent.Promise

/**
* This class represents a single waiting operation. These items shouldn't be reused or shared 
* since they don't have value semantics
* @author Mikko Hilpinen
* @since 31.3.2019
**/
@deprecated("Replaced with Wait", "v1.15")
class SingleWait(val target: WaitTarget) extends Runnable with Breakable
{
    // ATTRIBUTES    --------------------
    
    private val lock = new AnyRef()
    private val _started = new VolatileFlag()
    private val promise = Promise[Unit]()
    
    
    // COMPUTED    ----------------------
    
    /**
     * Whether this wait has already started
     */
    def hasStarted = _started.value
    
    /**
     * A future of the completion of this wait
     */
    def future = promise.future
    
    /**
     * Whether this wait has already ended
     */
    def hasEnded = promise.isCompleted
    
    
    // IMPLEMENTED    -------------------
    
    /**
     * Performs this wait (blocks). This method should be called once only.
     */
	def run() = 
	{
        // If this wait has already started, simply waits until the original run has completed
        if (_started.getAndSet())
            future.waitFor()
        else
        {
            // If not, performs the wait and then completes the promise
            target.breakable.waitWith(lock)
            promise.success(())
        }
	}
	
	def stop() = 
	{
	    target.notify(lock)
	    future
	}
}
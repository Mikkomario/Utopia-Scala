package utopia.flow.async

import scala.concurrent.Promise
import utopia.flow.util.WaitTarget
import utopia.flow.util.WaitUtils
import utopia.flow.util.WaitTarget.WaitDuration

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

object Loop
{
    /**
     * Creates a new looping operation
     * @param wait the wait time between iterations
     * @param operation the operation that is looped
     */
    def apply(wait: Duration)(operation: => Unit): Loop = new SimpleLoop(WaitDuration(wait), () => operation)
}

/**
* Loops are operations that can be repeated multpile times. Loops can also be broken between 
* operations. Loops don't have value semantics. One loop should only be used a single time by 
* a single instance.
* @author Mikko Hilpinen
* @since 31.3.2019
**/
trait Loop extends Runnable with Breakable
{
    // ATTRIBUTES    ---------------
    
    /**
     * The lock this loop should use whenever waiting
     */
    val waitLock = new AnyRef()
    
    private val breakFlag = new VolatileFlag()
    private val startedFlag = new VolatileFlag()
    private val promise = Promise[Unit]()
    
    
    // ABSTRACT    -----------------
    
    /**
     * Performs the operation in this loop once
     */
    protected def runOnce(): Unit
    
    /**
     * The time between the end of the current run and the start of the next one
     */
    protected def nextWaitTarget: WaitTarget
    
    
    // COMPUTED    -----------------
    
    /**
     * Whether this loop has started running
     */
    def hasStarted = startedFlag.isSet
    
    /**
     * Whether this loop has been broken
     */
    def isBroken = breakFlag.isSet
    
    /**
     * Whether this loop has finished
     */
    def hasEnded = promise.isCompleted
    
    
	// IMPLEMENTED    --------------
    
    def run() = 
    {
        startedFlag.set()
        
        while (!isBroken)
        {
            // Performs the operation
            runOnce()
            
            // Waits between runs
            if (!isBroken)
                nextWaitTarget.waitWith(waitLock)
        }
        
        // Finishes this loop
        promise.success(())
    }
    
    def stop() = 
    {
        // Breaks this loop and any waits
        breakFlag.set()
        WaitUtils.notify(waitLock)
        
        promise.future
    }
    
    
    // OTHER    ------------------
    
    /**
     * Starts this loop in an asynchronous context
     */
    def startAsync()(implicit context: ExecutionContext) = 
    {
        if (!hasStarted)
            context.execute(this)
    }
}

private class SimpleLoop(val nextWaitTarget: WaitTarget, val operation: () => Unit) extends Loop
{
    def runOnce() = operation()
}
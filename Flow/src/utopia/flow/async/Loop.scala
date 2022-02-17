package utopia.flow.async

import utopia.flow.time.{WaitTarget, WaitUtils}

import scala.concurrent.{ExecutionContext, Future, Promise}
import utopia.flow.time.WaitTarget.WaitDuration
import utopia.flow.util.CollectionExtensions._

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

object Loop
{
    // OTHER    -----------------------------
    
    /**
     * Creates a new looping operation
     * @param wait the wait time between iterations
     * @param operation the operation that is looped
     */
    def apply(wait: Duration)(operation: => Unit): Loop = new SimpleLoop(WaitDuration(wait), () => operation)
    
    
    // NESTED   -----------------------------
    
    private class SimpleLoop(val nextWaitTarget: WaitTarget, val operation: () => Unit) extends Loop
    {
        def runOnce() = operation()
    }
}

/**
* Loops are operations that can be repeated multpile times. Loops can also be broken between 
* operations. Loops don't have value semantics. One loop should only be used a single time by 
* a single instance.
* @author Mikko Hilpinen
* @since 31.3.2019
**/
// TODO: Add delayed start function
abstract class Loop extends Runnable with Breakable
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
    def runOnce(): Unit
    
    /**
     * The time between the end of the current run and the start of the next one
     */
    def nextWaitTarget: WaitTarget
    
    
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
    
    def run(): Unit = run(waitFirst = false)
    
    /**
      * @param waitFirst Whether operation should be delayed according to this loop's wait time
      */
    def run(waitFirst: Boolean): Unit =
    {
        startedFlag.set()
        
        // May perform a single wait before performing the first iteration
        if (waitFirst)
            nextWaitTarget.waitWith(waitLock)
        
        while (!isBroken)
        {
            // Performs the operation. Exceptions are only printed and not thrown forward.
            Try { runOnce() }.failure.foreach { _.printStackTrace() }
            
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
    def startAsync()(implicit context: ExecutionContext) = {
        if (!hasStarted)
            context.execute(this)
    }
    
    /**
      * Starts this loop after an initial delay. Takes into consideration the possibility of this loop being
      * broken during the delay, in which case won't start this loop.
      * @param delay Delay before starting this loop
      * @param context Implicit execution context
      * @return Future that resolves when this loop has started (or not started due to being stopped first)
      */
    def startAsyncAfter(delay: FiniteDuration)(implicit context: ExecutionContext) = {
        if (delay <= Duration.Zero) {
            startAsync()
            Future.successful(())
        }
        else if (!hasStarted)
            WaitUtils.delayed(delay) {
                if (!isBroken)
                    startAsync()
            }
        else
            Future.successful(())
    }
}
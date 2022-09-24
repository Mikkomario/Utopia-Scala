package utopia.flow.async.process

import utopia.flow.async.VolatileOption
import utopia.flow.time.WaitTarget.WaitDuration
import utopia.flow.time.WeekDay
import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.mutable.async.VolatileFlag

import java.time.LocalTime
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Try}

object Loop
{
    // OTHER    -----------------------------
    
    /**
      * Creates a new looping operation
      * @param wait the wait time between iterations
      * @param operation the operation that is looped
      */
    @deprecated("Please use LoopingProcess or .static(...)(...) instead")
    def apply(wait: Duration)(operation: => Unit): Loop = new SimpleLoop(WaitDuration(wait), () => operation)
    
    /**
      * Starts a new looping process after an initial delay
      * @param delay Delay before calling the specified function the first time
      * @param f The function that will be called regularly.
      *          Returns the next wait target or None if this loop should be broken afterwards.
      * @param exc Implicit execution context
      * @return The loop that was just started
      */
    def after(delay: WaitTarget)(f: => Option[WaitTarget])(implicit exc: ExecutionContext, logger: Logger) =
    {
        val loop = LoopingProcess(delay) { _ => f }
        loop.runAsync()
        loop
    }
    
    /**
      * Starts a new looping process
      * @param f The function that will be called regularly.
      *          Returns the next wait target or None if this loop should be broken afterwards.
      * @param exc Implicit execution context
      * @return The loop that was just started
      */
    def apply(f: => Option[WaitTarget])(implicit exc: ExecutionContext, logger: Logger): LoopingProcess =
        after(WaitTarget.zero)(f)
    
    /**
      * Starts a new infinite looping process
      * @param interval Delay between this loop's iterations
      * @param waitLock Wait lock to use (optional)
      * @param waitFirst Whether this loop should wait one interval before calling the specified function
      *                  (default = false)
      * @param f The function that will be called regularly
      * @param exc Implicit execution context
      * @return The started loop process
      */
    def regularly[U](interval: FiniteDuration, waitLock: AnyRef = new AnyRef, waitFirst: Boolean = false)
                 (f: => U)
                 (implicit exc: ExecutionContext, logger: Logger) =
    {
        val loop = LoopingProcess.static(interval, waitLock, waitFirst = waitFirst) { _ =>
            f
            Some(interval)
        }
        loop.runAsync()
        loop
    }
    
    /**
      * Starts a new infinite looping process that iterates once every day
      * @param runTime Time of day when this process should be run
      * @param f The function that will be called regularly.
      * @param exc Implicit execution context
      * @return The loop process that was just started
      */
    def daily[U](runTime: LocalTime)(f: => U)(implicit exc: ExecutionContext, logger: Logger) =
    {
        val loop = LoopingProcess.daily(runTime) { _ => f }
        loop.runAsync()
        loop
    }
    
    /**
      * Starts a new infinite looping process that iterates once every week
      * @param day Weekday on which the specified function should be run
      * @param time Time of day at which the specified function should be run (on targeted weekdays)
      * @param f The function that will be called regularly.
      * @param exc Implicit execution context
      * @return The loop process that was just started
      */
    def weekly[U](day: WeekDay, time: LocalTime)(f: => U)(implicit exc: ExecutionContext, logger: Logger) =
    {
        val loop = LoopingProcess.weekly(day, time) { _ => f }
        loop.runAsync()
        loop
    }
    
    /**
      * Performs the specified operation repeatedly until it succeeds
      * or until a specific amount of attempts has been made
      * @param firstInterval Interval between the first and the second attempt
      * @param maxAttempts Maximum number of attempts to make (> 0)
      * @param intervalModifier A modifier applied to the interval between consecutive attempts
      *                         (default = 1.0 = not modified)
      * @param f Attempt function that returns a success or a failure
      * @param exc Implicit execution context
      * @tparam A Type of successful function result
      * @return A future that completes when the specified function completes successfully or when enough failed
      *         attempts have been made (contains success or failure, accordingly)
      */
    def tryRepeatedly[A](firstInterval: FiniteDuration, maxAttempts: Int, intervalModifier: Double = 1.0)
                        (f: => Try[A])
                        (implicit exc: ExecutionContext) =
    {
        implicit val logger: Logger = SysErrLogger
        
        // The delay between attempts may be modified between attempts
        val intervalIterator = {
            if (intervalModifier == 1.0)
                Iterator.continually(firstInterval)
            else
                Iterator.iterate(firstInterval: Duration) { _ * intervalModifier }
        }.take(maxAttempts - 1)
        // Stores the function return value(s) to a pointer
        val resultPointer = VolatileOption[Try[A]]()
        // Starts looping in the background
        val loop = apply {
            val result = Try(f).flatten
            resultPointer.setOne(result)
            if (result.isSuccess)
                None
            else
                intervalIterator.nextOption().map { WaitDuration(_) }
        }
        // Returns a future that completes when the loop closes
        loop.completionFuture.map { _ =>
            resultPointer.value.getOrElse { Failure(new InterruptedException("No attempts")) }
        }
    }
    
    
    // NESTED   -----------------------------
    
    @deprecated("Please use LoopingProcess instead", "v1.16")
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
@deprecated("Please use LoopingProcess instead", "v1.15")
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
        
        while (!isBroken) {
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
    def startAsync()(implicit context: ExecutionContext) =
    {
        if (!hasStarted)
            context.execute(this)
    }
    
    /**
      * Starts this loop after an initial delay. Takes into consideration the possibility of this loop being
      * broken during the delay, in which case won't start this loop.
      * @param delay   Delay before starting this loop
      * @param context Implicit execution context
      * @return Future that resolves when this loop has started (or not started due to being stopped first)
      */
    def startAsyncAfter(delay: FiniteDuration)(implicit context: ExecutionContext) =
    {
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
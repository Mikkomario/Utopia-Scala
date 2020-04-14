package utopia.flow.util

import utopia.flow.util.TimeExtensions._
import java.time.Instant

import scala.concurrent.duration.Duration
import scala.util.Try

object WaitTarget
{
    /**
     * This waitTarget waits until the lock is notified
     */
    case object UntilNotified extends WaitTarget
    {
        protected val targetTime = Left(Duration.Inf)
        val breaksOnNotify = true
        
        def breakable = this
    }
    
    /**
     * This waitTarget always waits a specified duration (unless broken)
     */
    case class WaitDuration(duration: Duration, breaksOnNotify: Boolean = true) extends WaitTarget
    {
        protected val targetTime = Left(duration)
        
        def breakable: WaitDuration = if (breaksOnNotify) this else WaitDuration(duration)
    }
    
    /**
     * This waitTarget waits until a specified time instant (unless broken). Once that 
     * time instant is reached, no waiting is done anymore
     */
    case class Until(time: Instant, breaksOnNotify: Boolean = true) extends WaitTarget
    {
        protected val targetTime = Right(time)
        
        def breakable: Until = if (breaksOnNotify) this else Until(time)
    }
    
    /**
      * A zero length wait duration
      */
    val zero = WaitDuration(Duration.Zero)
}

/**
* A wait target specifies a waiting time: either until a specified instant, 
* a specific duration or an infinite time period
* @author Mikko Hilpinen
* @since 31.3.2019
**/
sealed trait WaitTarget
{
    // ABSTRACT    --------------
    
    protected def targetTime: Either[Duration, Instant]
    
    /**
     * Whether waits will stop once this wait target is notified
     */
    def breaksOnNotify: Boolean
    
    /**
     * A breakable version of this wait target that will stop once the lock is notified
     */
    def breakable: WaitTarget
    
    
	// COMPUTED    --------------
    
    /**
     * Whether this wait target has a maximum duration
     */
    def isInfinite = targetTime.left.exists { !_.isFinite }
    
    /**
     * Whether this wait target only stops when the lock is notified
     */
    def isFinite = !isInfinite
    
    /**
     * the duration of this target or None if this target had infinite duration
     */
    def toFiniteDuration = toDuration.finite
    
    /**
     * The duration of this target. May be infinite
     */
    def toDuration: Duration = targetTime match
    {
        case Left(duration) => duration
        case Right(time) => time - Instant.now()
    }
    
    /**
     * @return the ending time of this target, after which no waiting is done
     */
    def endTime = targetTime match
    {
        case Left(duration) => duration.finite.map { Instant.now() + _ }
        case Right(time) => Some(time)
    }
    
    /**
     * Whether this wait target is specified by wait duration
     */
    def durationIsSpecified = targetTime.isLeft
    
    /**
      * @return Whether this wait target is specified by a finite wait duration
      */
    def finiteDurationIsSpecified = targetTime.left.exists { _.isFinite }
    
    /**
     * Whether this wait target is specified by end time
     */
    def endTimeIsSpecified = targetTime.isRight
    
    
    // OTHER    -----------------
    
    /**
     * Blocks the current thread until this wait target is reached
     * @param lock the lock on which the waiting is done
     * @see notify(lock)
     */
    def waitWith(lock: AnyRef) = 
    {
        var waitCompleted = false
        
        if (isFinite)
        {
            val targetTime = endTime.get
            
            lock.synchronized
            {
                var currentTime = Instant.now()
                
                while (!waitCompleted && (currentTime < targetTime))
                {
                    val waitDuration = targetTime - currentTime
                    // Performs the actual wait here (nano precision)
                    // Exceptions are ignored
                    Try
                    {
                        lock.wait(waitDuration.toMillis, waitDuration.getNano / 1000)
                        
                        if (breaksOnNotify)
                            waitCompleted = true
                    }
                    
                    currentTime = Instant.now()
                }
            }
        }
        else
        {
            lock.synchronized
            {
                while (!waitCompleted)
                {
                    // Waits until notified, exceptions are ignored
                    Try
                    {
                        lock.wait()
                        waitCompleted = true
                    }
                }
            }
        }
    }
    
    /**
     * Notifies a lock, possibly breaking any wait that is waiting on that lock. Unbreakable waits 
     * won't be affected.
     */
    def notify(lock: AnyRef) = lock.synchronized { lock.notifyAll() }
    
    /**
     * Creates a new wait instance based on this wait target
     */
    def newWait() = new SingleWait(this)
}
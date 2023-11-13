package utopia.flow.async.process

import utopia.flow.operator.MayBeZero
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Now, Today, WeekDay}

import java.time.{Instant, LocalTime}
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.language.implicitConversions
import scala.util.Try

/**
* A wait target specifies a waiting time: either until a specified instant, 
* a specific duration or an infinite time period
* @author Mikko Hilpinen
* @since 31.3.2019
**/
sealed trait WaitTarget extends MayBeZero[WaitTarget]
{
    // ABSTRACT    --------------
    
    protected def targetTime: Either[Duration, Instant]
    
    /**
      * @return Whether this wait is more than 0 ms long
      */
    def isPositive: Boolean
    /**
     * Whether waits will stop once this wait target is notified
     */
    def breaksOnNotify: Boolean
    
    /**
     * A breakable version of this wait target that will stop once the lock is notified
     */
    def breakable: WaitTarget
    
    
	// COMPUTED    --------------
    
    override def zero = WaitTarget.zero
    
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
    def toDuration: Duration = targetTime match {
        case Left(duration) => duration
        case Right(time) => time - Now
    }
    
    /**
     * @return the ending time of this target, after which no waiting is done
     */
    def endTime = targetTime match {
        case Left(duration) => duration.finite.map { Now + _ }
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
    
    
    // IMPLEMENTED  -------------
    
    override def self = this
    override def isZero = !isPositive
    
    
    // OTHER    -----------------
    
    /**
     * Blocks the current thread until this wait target is reached
     * @param lock the lock on which the waiting is done
     * @see WaitUtils.notify(AnyRef)
     */
    @deprecated("Please use the Wait class instead, since it provides better control over the waiting and jvm shutdowns",
        "v1.15")
    def waitWith(lock: AnyRef) =
    {
        var waitCompleted = false
        
        if (isFinite) {
            val targetTime = endTime.get
            
            lock.synchronized {
                var currentTime = Instant.now()
                
                while (!waitCompleted && (currentTime < targetTime)) {
                    val waitDuration = targetTime - currentTime
                    // Performs the actual wait here (nano precision)
                    // Exceptions are ignored
                    Try {
                        lock.wait(waitDuration.toMillis, waitDuration.getNano / 1000)
                        
                        if (breaksOnNotify)
                            waitCompleted = true
                    }
                    
                    currentTime = Now
                }
            }
        }
        else {
            lock.synchronized {
                while (!waitCompleted)
                {
                    // Waits until notified, exceptions are ignored
                    Try {
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
    @deprecated("Please use WaitUtils.notify(AnyRef) instead", "v1.15")
    def notify(lock: AnyRef) = lock.synchronized { lock.notifyAll() }
    
    /**
     * Creates a new wait instance based on this wait target
      * @param lock Wait lock to use (default = new lock)
      * @param shutdownReaction Reaction to JVM shutdown (optional)
      * @param isRestartable Whether this wait instance should be completable multiple times
     */
    def newWait(lock: AnyRef = new AnyRef, shutdownReaction: Option[ShutdownReaction] = None,
                isRestartable: Boolean = true)(implicit exc: ExecutionContext) =
        new Wait(this, lock, shutdownReaction, isRestartable)
}

object WaitTarget
{
    // ATTRIBUTES   --------------------
    
    /**
      * A zero length wait duration
      */
    val zero = WaitDuration(Duration.Zero)
    
    
    // IMPLICIT ------------------------
    
    implicit def instantToWaitTarget(targetTime: Instant): WaitTarget = Until(targetTime)
    implicit def durationToWaitTarget(duration: Duration): WaitTarget = WaitDuration(duration)
    implicit def localTimeToWaitTarget(time: LocalTime): WaitTarget = DailyTime(time)
    
    
    // NESTED   ------------------------
    
    /**
      * This waitTarget waits until the lock is notified
      */
    case object UntilNotified extends WaitTarget
    {
        protected val targetTime = Left(Duration.Inf)
        val breaksOnNotify = true
    
        override def isPositive = true
    
        def breakable = this
    }
    
    /**
      * This waitTarget always waits a specified duration (unless broken)
      */
    case class WaitDuration(duration: Duration, breaksOnNotify: Boolean = true) extends WaitTarget
    {
        protected val targetTime = Left(duration)
    
        override def isPositive = duration > Duration.Zero
    
        def breakable: WaitDuration = if (breaksOnNotify) this else WaitDuration(duration)
    }
    
    /**
      * This waitTarget waits until a specified time instant (unless broken). Once that
      * time instant is reached, no waiting is done anymore
      */
    case class Until(time: Instant, breaksOnNotify: Boolean = true) extends WaitTarget
    {
        protected val targetTime = Right(time)
    
        override def isPositive = time.isFuture
    
        def breakable: Until = if (breaksOnNotify) this else Until(time)
    }
    
    /**
      * This wait target waits until specified local time today or the next day
      * (whichever is in future and comes first)
      * @param time Targeted time (local)
      * @param breaksOnNotify Whether this wait should be breakable using notify (default = true)
      */
    case class DailyTime(time: LocalTime, breaksOnNotify: Boolean = true) extends WaitTarget
    {
        override protected def targetTime =
        {
            val date = {
                if (Now.toLocalTime >= time)
                    Today.tomorrow
                else
                    Today.toLocalDate
            }
            Right(date.atTime(time).toInstantInDefaultZone)
        }
        override def isPositive = true
        override def breakable = copy(breaksOnNotify = true)
    }
    
    /**
      * This wait target waits until a specified time on the specified week day, repeating every 7 days.
      * @param weekDay Targeted weekday
      * @param time Targeted time on that weekday
      * @param breaksOnNotify Whether notify call should break the wait (default = true)
      */
    case class WeeklyTime(weekDay: WeekDay, time: LocalTime, breaksOnNotify: Boolean = true) extends WaitTarget
    {
        override protected def targetTime = {
            val date = Today.next(weekDay, includeSelf = Now.toLocalTime < time)
            Right(date.atTime(time).toInstantInDefaultZone)
        }
        override def isPositive = true
        override def breakable = copy(breaksOnNotify = true)
    }
}
package utopia.flow.view.immutable.caching

import utopia.flow.async.context.{CloseHook, Scheduler}
import utopia.flow.async.process.Loop
import utopia.flow.async.process.WaitTarget.Until
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Duration, Now}
import utopia.flow.util.logging.Logger
import utopia.flow.util.result.TryExtensions._
import utopia.flow.view.mutable.async.Volatile

import java.time.Instant
import scala.annotation.unchecked.uncheckedVariance
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.{Failure, Success, Try}

object ClosesAfterIdle
{
	// COMPUTED ----------------------------
	
	def factory(implicit scheduler: Scheduler, exc: ExecutionContext, log: Logger) = new ClosesAfterIdleFactory()
	
	
	// IMPLICIT     ------------------------
	
	implicit def objectAsFactory(o: ClosesAfterIdle.type)
	                            (implicit scheduler: Scheduler, exc: ExecutionContext, log: Logger): ClosesAfterIdleFactory =
		o.factory
	
	
	// NESTED   ----------------------------
	
	class ClosesAfterIdleFactory(addsShutdownHook: Boolean = false)
	                            (implicit scheduler: Scheduler, exc: ExecutionContext, log: Logger)
	{
		// COMPUTED ------------------------
		
		/**
		 * @return A copy of this factory that closes the open item at JVM shutdown
		 */
		def closingOnJvmShutdown = new ClosesAfterIdleFactory(addsShutdownHook = true)
		
		
		// OTHER    ------------------------
		
		/**
		 * @param idleThreshold Idle-threshold to apply
		 * @return A copy of this factory applying a static idle-threshold
		 */
		def after(idleThreshold: Duration) = new ClosesAfterIdleForFactory(idleThreshold, addsShutdownHook)
		
		/**
		 * Creates a new container that doesn't keep failed items
		 * @param open A function for opening a new item.
		 *             Besides returning the new item, returns the duration how long the item should be kept open.
		 *             May yield a failure.
		 * @tparam A Type of the opened items, on success
		 * @return A new container
		 */
		def trying[A <: AutoCloseable](open: => Try[(A, Duration)]) =
			tryingUsing(open) { _.close() }
		/**
		 * Creates a new container that doesn't keep failed items
		 * @param open A function for opening a new item.
		 *             Besides returning the new item, returns the duration how long the item should be kept open.
		 *             May yield a failure.
		 * @param close A function for closing an open item
		 * @tparam A Type of the opened items, on success
		 * @return A new container
		 */
		def tryingUsing[A](open: => Try[(A, Duration)])(close: A => Unit) =
			using {
				open match {
					case Success((value, idleThreshold)) => Success(value) -> idleThreshold
					case Failure(error) => Failure(error) -> Duration.zero
				}
			} { _.foreach(close) }
		
		/**
		 * Creates a new automatically closing wrapper
		 * @param open A function for opening a new item.
		 *             Besides returning the new item, returns the duration how long the item should be kept open.
		 * @tparam A Type of the wrapped item
		 * @return A new lazily initialized container
		 */
		def apply[A <: AutoCloseable](open: => (A, Duration)) = using(open) { _.close() }
		/**
		 * Creates a new automatically closing wrapper
		 * @param open A function for opening a new item.
		 *             Besides returning the new item, returns the duration how long the item should be kept open.
		 * @param close A function for closing a previously opened item
		 * @tparam A Type of the wrapped item
		 * @return A new lazily initialized container
		 */
		def using[A](open: => (A, Duration))(close: A => Unit) = new ClosesAfterIdle[A](addsShutdownHook)(open)(close)
	}
	
	class ClosesAfterIdleForFactory(idleThreshold: Duration, addsShutdownHook: Boolean = false)
	                               (implicit scheduler: Scheduler, exc: ExecutionContext, log: Logger)
	{
		// COMPUTED -------------------------
		
		/**
		 * @return A copy of this factory that closes the open item at JVM shutdown
		 */
		def closingOnJvmShutdown = new ClosesAfterIdleForFactory(idleThreshold, addsShutdownHook = true)
		
		
		// OTHER    ------------------------
		
		/**
		 * Creates a new container that doesn't keep failed items
		 * @param open A function for opening a new item. May yield a failure.
		 * @tparam A Type of the opened items, on success
		 * @return A new container
		 */
		def trying[A <: AutoCloseable](open: => Try[A]) = tryingUsing(open) { _.close() }
		/**
		 * Creates a new container that doesn't keep failed items
		 * @param open A function for opening a new item. May yield a failure.
		 * @param close A function for closing an open item
		 * @tparam A Type of the opened items, on success
		 * @return A new container
		 */
		def tryingUsing[A](open: => Try[A])(close: A => Unit) =
			new ClosesAfterIdle[Try[A]](addsShutdownHook)({
				val result = open
				val appliedIdleThreshold = if (result.isSuccess) idleThreshold else Duration.zero
				result -> appliedIdleThreshold
			})({ _.foreach(close) })
		
		/**
		 * Creates a new automatically closing wrapper
		 * @param open A function for opening a new item.
		 * @tparam A Type of the wrapped item
		 * @return A new lazily initialized container
		 */
		def apply[A <: AutoCloseable](open: => A) = using(open) { _.close() }
		/**
		 * Creates a new automatically closing wrapper
		 * @param open A function for opening a new item.
		 * @param close A function for closing a previously opened item
		 * @tparam A Type of the wrapped item
		 * @return A new lazily initialized container
		 */
		def using[A](open: => A)(close: A => Unit) =
			new ClosesAfterIdle[A](addsShutdownHook)({ open -> idleThreshold })(close)
	}
}

/**
 * A lazily initialized container that closes after not used for a while
 * @tparam A Type of the wrapped item
 * @param addShutdownHook Whether to register a shutdown-hook that closes the wrapped item, if open.
 *                        Default = false.
 * @param open A function for opening a new item.
 *             Besides returning the new item, returns the duration how long the item should be kept open.
 * @param close A function for closing a previously opened item
 * @param scheduler Implicit scheduler used
 * @param exc Execution context used for setting up the possible close-hook
 * @param log Implicit logging implementation used
 * @author Mikko Hilpinen
 * @since 08.05.2026, v2.9
 */
class ClosesAfterIdle[+A](addShutdownHook: Boolean = false)
                         (open: => (A, Duration))(close: A => Unit)
                         (implicit scheduler: Scheduler, exc: ExecutionContext, log: Logger)
	extends Lazy[A]
{
	// ATTRIBUTES   ----------------------
	
	private var lastIdleThreshold = Duration.zero
	
	/**
	 * A pointer that contains >0 while the wrapped item should not close
	 * (i.e. while executing some longer-running function)
	 */
	private val lockCounter = Volatile(0)
	/**
	 * A pointer that contains the last time [[value]] was called
	 */
	private val lastUsedP = Volatile.eventful(Now.toInstant)
	/**
	 * A pointer that contains the last reset-loop.
	 * Used for checking whether a new loop should be started.
	 */
	private val resetLoopP = Volatile(Future.unit)
	
	/**
	 * A pointer that contains the wrapped item, while it is open.
	 */
	private val wrappedP: Volatile[Option[A @uncheckedVariance]] = Volatile.empty
	
	
	// INITIAL CODE ----------------------
	
	// Adds the shutdown hook, if appropriate
	if (addShutdownHook)
		CloseHook.registerAction { reset() }
	
	
	// IMPLEMENTED  ----------------------
	
	override def current: Option[A] = {
		lastUsedP.value = Now
		wrappedP.value
	}
	
	override def value: A = {
		val now = Now.toInstant
		// Marks the item as just used
		lastUsedP.value = now
		val (result, newIdleThreshold) = wrappedP.mutate {
			// Case: Open => Yields the open item
			case current @ Some(item) => (item, None) -> current
			// Case: Not open => Opens a new item and prepares a loop for closing it
			case None =>
				val (newItem, idleThreshold) = open
				(newItem, Some(idleThreshold)) -> Some(newItem)
		}
		// Starts a reset-loop, if a new item was generated
		newIdleThreshold.foreach { idleThreshold =>
			lastIdleThreshold = idleThreshold
			startNewResetLoop(now, idleThreshold)
		}
		
		result
	}
	
	
	// OTHER    --------------------------
	
	/**
	 * Keeps the wrapped item open while executing the specified function
	 * @param f A function to execute using an open item
	 * @tparam R Type of 'f' results
	 * @return Results of 'f'
	 */
	def keepOpenDuring[R](f: A => R) = {
		// Locks
		lockCounter.update { _ + 1 }
		// Executes the wrapped function
		try {
			val result = f(value)
			lastUsedP.value = Now
			result
		}
		finally {
			// Case: The reset loop had finished while locked => Starts a new reset loop or closes the wrapped item
			if (lockCounter.updateAndGet { _ - 1 } <= 0 && resetLoopP.value.isCompleted) {
				val now = Now.toInstant
				// Case: Item should be closed => Closes it
				if ((lastUsedP.value + lastIdleThreshold) <= now)
					reset()
				// Case: Item should be kept open => Prepares a loop to close it
				else
					startNewResetLoop(now, lastIdleThreshold)
			}
		}
	}
	/**
	 * Keeps the wrapped item open until the specified function has fully resolved
	 * @param f A function to execute using an open item. Yields a future.
	 * @tparam R Type of eventual 'f' results
	 * @return Result of 'f'
	 */
	def keepOpenUntilCompletionOf[R](f: A => Future[R]) = {
		// Locks
		lockCounter.update { _ + 1 }
		// Executes the wrapped function
		val future = Try { f(value) }.getOrMap(Future.failed)
		// Once the function has fully resolved, releases the lock
		future.onComplete { _ =>
			val now = Now.toInstant
			lastUsedP.value = now
			// Also restarts the reset loop, if appropriate
			if (lockCounter.updateAndGet { _ - 1 } <= 0 && resetLoopP.value.isCompleted)
				startNewResetLoop(now, lastIdleThreshold)
		}
		
		future
	}
	
	private def startNewResetLoop(now: Instant, idleThreshold: Duration) = {
		if (idleThreshold.isPositive) {
			// Case: Finite positive idle-threshold => Schedules the reset using a loop
			if (idleThreshold.isFinite) {
				val invalidatesP = lastUsedP.map { _ + idleThreshold }
				resetLoopP.value = Loop.after(now + idleThreshold) {
					val nextWaitTarget = Some(invalidatesP.value).filter { _.isFuture }
					
					// Applies the reset, if appropriate
					// NB: Won't reset while locked
					if (nextWaitTarget.isEmpty && lockCounter.value <= 0)
						reset()
					
					nextWaitTarget.map { Until(_) }
				}
			}
			// Case: Infinite idle threshold => Won't schedule a reset loop
			else
				resetLoopP.value = Future.never
		}
		// Case: Zero or negative idle threshold => Resets immediately, unless locked
		else {
			resetLoopP.value = Future.unit
			if (lockCounter.value <= 0)
				reset()
		}
	}
	
	private def reset() =
		wrappedP.pop().foreach { item => Try { close(item) }.logWithMessage("Failure while closing") }
}

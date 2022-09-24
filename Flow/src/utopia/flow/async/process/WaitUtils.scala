package utopia.flow.async.process

import utopia.flow.async.process
import utopia.flow.async.process.WaitTarget.{Until, UntilNotified, WaitDuration}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.SysErrLogger

import java.time.Instant
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

/**
  * WaitUtils contains a number of utility tools for waiting on a thread. This utility object handles
  * possible interruptedExceptions as well as synchronization
  * @author Mikko Hilpinen
  * @since 8.2.2018
  */
object WaitUtils
{
	// OTHER    ---------------------------
	
	/**
	  * Waits the duration of the specified wait target
	  */
	@deprecated("Please use the Wait class instead, since it provides better control over the waiting and jvm shutdowns",
		"v1.15")
	def wait(target: WaitTarget, lock: AnyRef = new AnyRef) = target.waitWith(lock)
	
	/**
	  * Waits for a certain amount of time (blocking), then releases the lock
	  */
	@deprecated("Please use the Wait class instead, since it provides better control over the waiting and jvm shutdowns",
		"v1.15")
	def wait(duration: Duration, lock: AnyRef): Unit = wait(WaitDuration(duration), lock)
	
	/**
	  * Waits for a certain amount of time (blocking), then releases the lock
	  */
	@deprecated("Please use the Wait class instead, since it provides better control over the waiting and jvm shutdowns",
		"v1.15")
	def wait(duration: java.time.Duration, lock: AnyRef): Unit = wait(WaitDuration(duration), lock)
	
	/**
	  * Waits until the lock is notified
	  * @see #notify(AnyRef)
	  */
	@deprecated("Please use the Wait class instead, since it provides better control over the waiting and jvm shutdowns",
		"v1.15")
	def waitUntilNotified(lock: AnyRef) = wait(UntilNotified, lock)
	
	/**
	  * Notifies the lock, so that threads waiting on it will be released
	  * @see #waitUntilNotified(AnyRef)
	  */
	def notify(lock: AnyRef) = lock.synchronized { lock.notifyAll() }
	
	/**
	  * Waits until the specified time has been reached
	  * @param targetTime The time until which waiting should occur
	  * @param lock       A lock instance that can be used to interrupt the wait by calling .notify(AnyRef)
	  */
	@deprecated("Please use the Wait class instead, since it provides better control over the waiting and jvm shutdowns",
		"v1.15")
	def waitUntil(targetTime: Instant, lock: AnyRef = new AnyRef) = wait(Until(targetTime), lock)
	
	/**
	  * Performs an operation asynchronously after a delay
	  * @param waitDuration Duration to wait before performing specified operation
	  * @param lock         Lock used for waiting (default = new object)
	  * @param operation    Operation performed after wait
	  * @param exc          Implicit execution context
	  * @tparam A Type of operation result
	  * @return Future of the completion of the operation
	  */
	@deprecated("Please use Delay instead", "v1.15")
	def delayed[A](waitDuration: Duration, lock: AnyRef = new AnyRef)
	              (operation: => A)
	              (implicit exc: ExecutionContext) =
		process.Delay(waitDuration, lock)(operation)(exc, SysErrLogger)
}

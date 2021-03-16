package utopia.flow.time

import utopia.flow.time.WaitTarget.{Until, UntilNotified, WaitDuration}
import TimeExtensions._

import java.time.Instant
import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * WaitUtils contains a number of utility tools for waiting on a thread. This utility object handles
  * possible interruptedExceptions as well as synchronization
  * @author Mikko Hilpinen
  * @since 8.2.2018
  */
object WaitUtils
{
	/**
	  * Waits the duration of the specified wait target
	  */
	def wait(target: WaitTarget, lock: AnyRef) = target.waitWith(lock)
	
	/**
	  * Waits for a certain amount of time (blocking), then releases the lock
	  */
	def wait(duration: Duration, lock: AnyRef): Unit = wait(WaitDuration(duration), lock)
	
	/**
	  * Waits for a certain amount of time (blocking), then releases the lock
	  */
	def wait(duration: java.time.Duration, lock: AnyRef): Unit = wait(WaitDuration(duration), lock)
	
	/**
	  * Waits until the lock is notified
	  * @see #notify(AnyRef)
	  */
	def waitUntilNotified(lock: AnyRef) = wait(UntilNotified, lock)
	
	/**
	  * Notifies the lock, so that threads waiting on it will be released
	  * @see #waitUntilNotified(AnyRef)
	  */
	def notify(lock: AnyRef) = lock.synchronized { lock.notifyAll() }
	
	/**
	  * Waits until the specified time has been reached
	  */
	def waitUntil(targetTime: Instant, lock: AnyRef) = wait(Until(targetTime), lock)
	
	/**
	  * Performs an operation asynchronously after a delay
	  * @param waitDuration Duration to wait before performing specified operation
	  * @param lock         Lock used for waiting (default = new object)
	  * @param operation    Operation performed after wait
	  * @param exc          Implicit execution context
	  * @tparam A Type of operation result
	  * @return Future of the completion of the operation
	  */
	def delayed[A](waitDuration: Duration, lock: AnyRef = new AnyRef)(operation: => A)(implicit exc: ExecutionContext) = Future {
		wait(waitDuration, lock)
		operation
	}
}

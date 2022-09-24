package utopia.flow.view.mutable.caching

import utopia.flow.async.process.Process
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.flow.view.immutable.caching.Lazy

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

object ExpiringLazy
{
	/**
	  * Creates a new lazy container that automatically resets its contents after
	  * a specified duration has passed from value generation
	  * @param expirationThreshold How long values are cached within this lazy
	  * @param make Function for generating a new value when one is requested
	  * @param exc Implicit execution context (used for scheduling the reset)
	  * @tparam A Type of cached value
	  * @return A new lazy container
	  */
	def after[A](expirationThreshold: FiniteDuration)(make: => A)(implicit exc: ExecutionContext) =
		new ExpiringLazy[A](make)(_ => expirationThreshold)
	/**
	  * Creates a new lazy container that automatically resets its contents after
	  * a specified duration has passed from value generation
	  * @param expirationThreshold How long values are cached within this lazy -
	  *                            If infinite, creates a standard lazy container
	  * @param make Function for generating a new value when one is requested
	  * @param exc Implicit execution context (used for scheduling the reset, if applicable)
	  * @tparam A Type of cached value
	  * @return A new lazy container
	  */
	def after[A](expirationThreshold: Duration)(make: => A)(implicit exc: ExecutionContext) =
		expirationThreshold.finite match
		{
			case Some(duration) => new ExpiringLazy[A](make)(_ => duration)
			case None => Lazy(make)
		}
	
	/**
	  * Creates a new lazy container that automatically resets its contents after
	  * a specified duration has passed from value generation
	  * @param make Function for generating a new value when one is requested
	  * @param calculateDuration Function for calculating a store duration for each item
	  * @param exc Implicit execution context (used for scheduling the reset)
	  * @tparam A Type of cached value
	  * @return A new lazy container
	  */
	def apply[A](make: => A)(calculateDuration: A => Duration)(implicit exc: ExecutionContext) =
		new ExpiringLazy[A](make)(calculateDuration)
}

/**
  * A lazy container that automatically resets its contents after a while.
  * Resetting is performed asynchronously.
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.10
  */
class ExpiringLazy[+A](generator: => A)(expirationPerItem: A => Duration)
                      (implicit exc: ExecutionContext)
	extends ResettableLazyLike[A]
{
	// ATTRIBUTES   --------------------------
	
	private implicit val log: Logger = SysErrLogger
	
	private val cache: ResettableLazyLike[A] = ResettableVolatileLazy(generator)
	private val expirationProcessPointer = VolatileOption[Process]()
	
	
	// IMPLEMENTED  --------------------------
	
	override def reset() = {
		// Cancels the scheduled expiration
		expirationProcessPointer.pop().foreach { _.stop() }
		cache.reset()
	}
	
	override def current = cache.current
	
	override def value = current match {
		case Some(value) => value
		case None =>
			// Acquires the new value
			val newValue = cache.value
			// Checks when the value should be reset
			expirationPerItem(newValue).finite.foreach { waitDuration =>
				// Case: Reset scheduled => schedules a reset
				if (waitDuration > Duration.Zero) {
					val newExpiration = DelayedProcess.hurriable(Now + waitDuration) { _ => cache.reset() }
					// If there was another reset pending, cancels that first
					expirationProcessPointer.getAndSet { Some(newExpiration) }.foreach { _.stopIfRunning() }
					newExpiration.runAsync()
				}
				// Case: The item is immediately marked as expired =>
				// resets the underlying cache without scheduling a new reset
				else
					cache.reset()
			}
			newValue
	}
}

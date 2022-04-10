package utopia.flow.datastructure.mutable

import utopia.flow.async.{ResettableVolatileLazy, Wait}
import utopia.flow.datastructure.immutable.Lazy
import utopia.flow.time.{Now, WaitUtils}
import utopia.flow.time.TimeExtensions._

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
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
  * A lazy container that automatically resets its contents after a while. Resetting is performed asynchronously.
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.10
  */
class ExpiringLazy[+A](generator: => A)(expirationPerItem: A => Duration)
                      (implicit exc: ExecutionContext) extends ResettableLazyLike[A]
{
	// ATTRIBUTES   --------------------------
	
	private val waitLock = new AnyRef
	private val cache: ResettableLazyLike[A] = ResettableVolatileLazy(generator)
	
	private var nextWaitThreshold: Option[Instant] = None
	
	
	// COMPUTED ------------------------------
	
	def isResetting = nextWaitThreshold.isDefined
	
	
	// IMPLEMENTED  --------------------------
	
	override def reset() =
	{
		_reset()
		WaitUtils.notify(waitLock)
	}
	
	override def current = cache.current
	
	override def value = current match
	{
		case Some(value) => value
		case None =>
			// Acquires the new value
			val newValue = cache.value
			// Schedules a reset event if one is not scheduled already (otherwise extends the wait)
			expirationPerItem(newValue).finite.foreach { waitDuration =>
				val wasWaiting = nextWaitThreshold.isDefined
				nextWaitThreshold = Some(Now + waitDuration)
				if (!wasWaiting)
				{
					Future {
						while (nextWaitThreshold.exists { Now < _ }) {
							nextWaitThreshold.foreach { Wait(_, waitLock) }
						}
						_reset()
					}
				}
			}
			newValue
	}
	
	
	// OTHER    ------------------------------
	
	private def _reset() =
	{
		nextWaitThreshold = None
		cache.reset()
	}
}

package utopia.flow.datastructure.mutable

import utopia.flow.time.{Now, WaitUtils}

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

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
	def apply[A](expirationThreshold: FiniteDuration)(make: => A)(implicit exc: ExecutionContext) =
		new ExpiringLazy[A](expirationThreshold)(make)
}

/**
  * A lazy container that automatically resets its contents after a while
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.10
  */
class ExpiringLazy[+A](expirationThreshold: FiniteDuration)(generator: => A)
                      (implicit exc: ExecutionContext) extends ResettableLazyLike[A]
{
	// ATTRIBUTES   --------------------------
	
	private val waitLock = new AnyRef
	private val cache: ResettableLazyLike[A] = ResettableLazy(generator)
	
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
			scheduleReset()
			cache.value
	}
	
	
	// OTHER    ------------------------------
	
	private def _reset() =
	{
		nextWaitThreshold = None
		cache.reset()
	}
	
	private def scheduleReset() =
	{
		if (!isResetting)
		{
			val newThreshold = Now + expirationThreshold
			nextWaitThreshold = Some(newThreshold)
			Future {
				while (nextWaitThreshold.exists { Now < _ })
				{
					nextWaitThreshold.foreach { WaitUtils.waitUntil(_, waitLock) }
				}
				_reset()
			}
		}
	}
}

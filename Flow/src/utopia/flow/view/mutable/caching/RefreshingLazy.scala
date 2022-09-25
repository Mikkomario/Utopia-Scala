package utopia.flow.view.mutable.caching

import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

import java.time.Instant
import scala.concurrent.duration.{Duration, FiniteDuration}

object RefreshingLazy
{
	/**
	  * Creates a new lazy container which refreshes its contents after it has existed for the specified time
	  * @param threshold Item maximum duration
	  * @param make A function for generating new items
	  * @tparam A Type of stored item
	  * @return A new refreshing lazy container
	  */
	def after[A](threshold: FiniteDuration)(make: => A) = new RefreshingLazy[A](make)(_ => threshold)
	/**
	  * Creates a new lazy container which refreshes its contents after it has existed for the specified time
	  * @param threshold Item maximum duration - If infinite, creates a simpler lazy container
	  * @param make A function for generating new items
	  * @tparam A Type of stored item
	  * @return A new lazy container
	  */
	def after[A](threshold: Duration)(make: => A): ResettableLazy[A] = threshold.finite match
	{
		case Some(finiteDuration) => after(finiteDuration)(make)
		case None => ResettableLazy(make)
	}
	
	/**
	  * Creates a new lazy container which considers its value expired after a while
	  * @param make A function for generating new values
	  * @param calculateDuration A function for calculating a lifespan for a cached value
	  * @tparam A Type of cached value
	  * @return A new lazy container
	  */
	def apply[A](make: => A)(calculateDuration: A => Duration) = new RefreshingLazy[A](make)(calculateDuration)
}

/**
  * A lazy container which refreshes its contents every once in a while, calculating a new value upon some requests.
  * @author Mikko Hilpinen
  * @since 29.10.2021, v1.14
  */
class RefreshingLazy[+A](generator: => A)(expirationPerItem: A => Duration) extends ResettableLazy[A]
{
	// ATTRIBUTES   ---------------------------
	
	private val cache: ResettableLazy[A] = ResettableLazy(generator)
	// None when not calculated
	// Some(None) when infinite
	// Some(Some(Instant)) when finite
	private var nextResetThreshold: Option[Option[Instant]] = None
	
	
	// IMPLEMENTED  ---------------------------
	
	override def reset() = {
		nextResetThreshold = None
		cache.reset()
	}
	
	override def current =
	{
		if (nextResetThreshold.forall { _.forall { _.isInFuture } })
			cache.current
		else
			None
	}
	override def value =
	{
		// Resets value first, if necessary
		// Case: Current value is no longer valid => Requests a new value and schedules a new refresh
		if (nextResetThreshold.forall { _.exists { _.isInPast } })
		{
			val result = cache.newValue()
			nextResetThreshold = Some(expirationPerItem(result).finite.map { Now + _ })
			result
		}
		// Case: Current value is still valid => uses that
		else
			cache.value
	}
}

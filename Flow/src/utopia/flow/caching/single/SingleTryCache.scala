package utopia.flow.caching.single

import utopia.flow.time.Now

import java.time.Instant
import utopia.flow.util.RichComparable._

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success, Try}

object SingleTryCache
{
	/**
	  * Creates a new single try cache
	  * @param failCacheDuration The duration which a failed result will be cached
	  * @param makeRequest A function for making a new request (call by name)
	  * @tparam A The type of function result on success
	  * @return A new cache
	  */
	def apply[A](failCacheDuration: FiniteDuration)(makeRequest: => Try[A]): SingleTryCache[A] =
		new SingleTryCacheImpl[A](failCacheDuration, () => makeRequest)
	
	/**
	 * Creates an expiring cache where failed requests have a shorter expiration time
	 * @param failCacheDuration How long failed requests are cached
	 * @param maxCacheDuration How long successful requests are cached
	 * @param makeRequest A function for making a request
	 * @tparam A Type of item cached
	 * @return A new cache
	 */
	def expiring[A](failCacheDuration: FiniteDuration, maxCacheDuration: FiniteDuration)(makeRequest: => Try[A]) =
		ExpiringSingleCache.wrap(apply(failCacheDuration){ makeRequest }, maxCacheDuration)
}

/**
  * This cache may succeed or fail in requesting an item. A failure will be cached only for a certain period of time
  * while a success is cached indefinitely
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
trait SingleTryCache[A] extends ClearableSingleCacheLike[Try[A]]
{
	// ATTRIBUTES	----------------
	
	private var success: Option[Success[A]] = None
	private var lastFailure: Option[(Failure[A], Instant)] = None
	
	
	// ABSTRACT	--------------------
	
	/**
	  * The duration which this cache holds its value
	  */
	protected val failCacheDuration: FiniteDuration
	
	/**
	  * @return Requests a new value for this cache
	  */
	protected def request(): Try[A]
	
	
	// COMPUTED	--------------------
	
	/**
	  * @return Whether a failure is currently cached
	  */
	def isFailureCached = lastFailure.exists { _._2 > Now - failCacheDuration }
	
	/**
	  * @return Whether a success is currently cached
	  */
	def isSuccessCached = success.isDefined
	
	private def cachedFailure = lastFailure.filter { _._2 > Now - failCacheDuration }.map { _._1 }
	
	
	// IMPLEMENTED	----------------
	
	override def cached = success orElse cachedFailure
	
	override def clear() =
	{
		lastFailure = None
		success = None
	}
	
	// Uses a cached failure if one is still in effect
	override def apply() =
	{
		success.getOrElse
		{
			cachedFailure.getOrElse
			{
				request() match
				{
					case s: Success[A] =>
						success = Some(s)
						s
					case f: Failure[A] =>
						lastFailure = Some(f -> Now)
						f
				}
			}
		}
	}
}

private class SingleTryCacheImpl[A](override protected val failCacheDuration: FiniteDuration,
									private val makeRequest: () => Try[A]) extends SingleTryCache[A]
{
	override protected def request() = makeRequest()
}
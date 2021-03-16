package utopia.flow.caching.single

import utopia.flow.time.Now

import java.time.Instant
import utopia.flow.time.TimeExtensions._

import scala.concurrent.duration.FiniteDuration

object ExpiringSingleCache
{
	/**
	  * Creates a new expiring single item cache
	  * @param cacheDuration The duration which each item is kept in this cache
	  * @param makeRequest A function for requesting a new item (call by name)
	  * @tparam A The type of returned item
	  * @return A new expiring single item cache
	  */
	def apply[A](cacheDuration: FiniteDuration)(makeRequest: => A): ExpiringSingleCache[A] =
		new ExpiringSingleCacheImpl(cacheDuration, ClearableSingleCache(makeRequest))
	
	/**
	  * Wraps a clearable cache, providing expiration features
	  * @param cache The wrapped cache
	  * @param cacheDuration The duration which a result will last before it expires
	  * @tparam A The type of result item
	  * @return A new expiring single item cache that uses the provided cache
	  */
	def wrap[A](cache: ClearableSingleCacheLike[A], cacheDuration: FiniteDuration): ExpiringSingleCache[A] =
		new ExpiringSingleCacheImpl(cacheDuration, cache)
}

/**
  * This cache's value expires after a while, after which a new value will be requested
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
trait ExpiringSingleCache[+A] extends ExpiringSingleCacheLike[A]
{
	// ATTRIBUTES	---------------
	
	private var dataOriginTime: Option[Instant] = None
	
	
	// ABSTRACT	-------------------
	
	/**
	  * @return The cache used for holding the value of this cache
	  */
	protected def cache: ClearableSingleCacheLike[A]
	
	/**
	  * The duration which an item is cached
	  */
	protected val cacheDuration: FiniteDuration
	
	
	// IMPLEMENTED	---------------
	
	def isDataExpired = dataOriginTime.exists { _ < Now - cacheDuration }
	
	override def apply() =
	{
		// May clear the underlying cache
		clearIfExpired()
		
		if (!cache.isValueCached)
			dataOriginTime = Some(Now.toInstant)
		
		cache()
	}
	
	override def cached = if (isDataExpired) None else cache.cached
	
	def clearIfExpired() = if (isDataExpired) cache.clear()
	
	override def clear() = cache.clear()
}

private class ExpiringSingleCacheImpl[+A](protected val cacheDuration: FiniteDuration,
										 protected val cache: ClearableSingleCacheLike[A]) extends ExpiringSingleCache[A]
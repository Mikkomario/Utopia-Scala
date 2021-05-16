package utopia.flow.caching.multi

import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

object Cache
{
	/**
	  * Creates a new cache
	  * @param request A function for retrieving the cached value
	  * @tparam Key The cache key type
	  * @tparam Value The type of cached result
	  * @return A new cache
	  */
	def apply[Key, Value](request: Key => Value) = new Cache[Key, Value](request)
	
	/**
	  * Creates a weakly referencing cache
	  * @param request Function for requesting new values
	  * @tparam K Type of keys used
	  * @tparam V Type of values used
	  * @return A new cache
	  */
	def weak[K, V <: AnyRef](request: K => V) = WeakCache(request)
	
	/**
	  * Creates a cache that removes cached items after a while
	  * @param request Function for requesting new values
	  * @param calculateExpiration A function for calculating item expiration time
	  * @tparam K Type of keys used
	  * @tparam V Type of values used
	  * @return A new cache
	  */
	def expiring[K, V](request: K => V)(calculateExpiration: (K, V) => Duration)(implicit exc: ExecutionContext) =
		ExpiringCache(request)(calculateExpiration)
	
	/**
	  * Creates a temporarily caching cache
	  * @param threshold A time threshold after which items are removed
	  * @param request Function for requesting new values
	  * @tparam K Type of keys used
	  * @tparam V Type of values used
	  * @return A new cache
	  */
	def expiringAfter[K, V](threshold: FiniteDuration)(request: K => V)(implicit exc: ExecutionContext) =
		ExpiringCache.after(threshold)(request)
	
	/**
	  * Creates a cache that uses weak references after a while
	  * @param request Function for requesting new values
	  * @param calculateReferenceLength A function for calculating the length of time to use for
	  *                                 holding a strong reference
	  * @tparam K Type of keys used
	  * @tparam V Type of values used
	  * @return A new cache
	  */
	def releasing[K, V <: AnyRef](request: K => V)(calculateReferenceLength: (K, V) => Duration)
	                             (implicit exc: ExecutionContext) =
		ReleasingCache(request)(calculateReferenceLength)
	
	/**
	  * Creates a cache that uses weak references after a while
	  * @param threshold A time threshold after which weak references are used
	  * @param request Function for requesting new values
	  * @tparam K Type of keys used
	  * @tparam V Type of values used
	  * @return A new cache
	  */
	def releasingAfter[K, V <: AnyRef](threshold: FiniteDuration)(request: K => V)(implicit exc: ExecutionContext) =
		ReleasingCache.after(threshold)(request)
}

/**
  * This is a simple implementation of the CacheLike trait
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
class Cache[Key, Value](private val request: Key => Value) extends CacheLike[Key, Value]
{
	// ATTRIBUTES	------------------
	
	private var cachedItems: Map[Key, Value] = HashMap()
	
	
	// IMPLEMENTED	------------------
	
	override def apply(key: Key) =
	{
		if (cachedItems.contains(key))
			cachedItems(key)
		else
		{
			val value = request(key)
			cachedItems += key -> value
			value
		}
	}
	
	override def cached(key: Key) = cachedItems.get(key)
}

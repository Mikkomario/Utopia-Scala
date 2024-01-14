package utopia.flow.collection.immutable.caching.cache

import utopia.flow.collection.template.MapAccess

import scala.collection.mutable
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
	def apply[Key, Value](request: Key => Value): Cache[Key, Value] = new _Cache[Key, Value](request)
	
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
	def expiring[K, V](request: K => V)(calculateExpiration: (K, V) => Duration)
	                  (implicit exc: ExecutionContext) =
		ExpiringCache(request)(calculateExpiration)
	
	/**
	  * Creates a temporarily caching cache
	  * @param threshold A time threshold after which items are removed
	  * @param request Function for requesting new values
	  * @tparam K Type of keys used
	  * @tparam V Type of values used
	  * @return A new cache
	  */
	def expiringAfter[K, V](threshold: FiniteDuration)(request: K => V)
	                       (implicit exc: ExecutionContext) =
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
	def releasingAfter[K, V <: AnyRef](threshold: FiniteDuration)
	                                  (request: K => V)
	                                  (implicit exc: ExecutionContext) =
		ReleasingCache.after(threshold)(request)
	
	/**
	  * Creates a new cache that only caches the latest requested value
	  * @param f A function for generating values for requested keys
	  * @tparam K Type of keys used
	  * @tparam V Type of values returned
	  * @return A new cache
	  */
	def onlyLatest[K, V](f: K => V) = CacheLatest(f)
		
	
	// NESTED   ---------------------------
	
	private class _Cache[Key, Value](request: Key => Value) extends Cache[Key, Value]
	{
		// ATTRIBUTES	------------------
		
		private val cachedItems: mutable.Map[Key, Value] = mutable.Map()
		
		
		// IMPLEMENTED	------------------
		
		override def cachedValues = cachedItems.values
		
		override def apply(key: Key) = cachedItems.getOrElseUpdate(key, request(key))
		override def cached(key: Key) = cachedItems.get(key)
	}
}

/**
  * Caches keep their values after requesting them
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
trait Cache[-Key, +Value] extends MapAccess[Key, Value]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return All the items currently stored in this cache
	  */
	def cachedValues: Iterable[Value]
	
	/**
	  * The currently cached value for specified key
	  * @param key A key
	  * @return The currently cached value for the key. None if no data is cached
	  */
	def cached(key: Key): Option[Value]
	
	
	// OTHER    --------------------------
	
	/**
	  * @param key A key
	  * @return Whether there is currently cached data for the key
	  */
	def isValueCached(key: Key) = cached(key).isDefined
	
	/**
	  * @param f A mapping function applied for values of this cache
	  * @tparam O  Type of values accepted by the mapping function (i.e. Value of this cache or higher type)
	  * @tparam V2 Type of values returned by mapping function
	  * @return A view of this cache that maps returned values (but doesn't cache mapped values)
	  */
	def mapValuesView[O >: Value, V2](f: O => V2) = MappingCacheView[Key, O, V2](this)(f)
	/**
	  * @param f A mapping function applied for key inputs to this cache, producing a key applicable to this cache
	  * @tparam K2 Type of keys accepted by the resulting cache (map input)
	  * @tparam OK Type of keys produced by the mapping function (map output, must be a sub-type of this cache's key)
	  * @return A copy of this cache that maps the incoming keys before proposing them to this cache
	  */
	def mapKeys[K2, OK <: Key](f: K2 => OK) = KeyMappingCache[K2, OK, Value](this)(f)
}
package utopia.flow.collection.immutable.caching.cache

import utopia.flow.collection.template.MapAccess
import utopia.flow.view.mutable.caching.ClearableCache

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object Cache
{
	// COMPUTED -------------------------
	
	/**
	  * @return Access to clearable cache constructors
	  */
	def clearable = ClearableCache
	/**
	  * Access to weak cache constructors
	  */
	def weak = WeakCache
	/**
	  * @return Access to expiring cache constructors
	  */
	def expiring = ExpiringCache
	/**
	  * @return Access to releasing cache constructors
	  */
	def releasing = ReleasingCache
	/**
	  * @return Access to cache latest -constructors
	  */
	def onlyLatest = CacheLatest
	
	
	// OTHER    -------------------------
	
	/**
	  * Creates a new cache
	  * @param request A function for retrieving the cached value
	  * @tparam Key The cache key type
	  * @tparam Value The type of cached result
	  * @return A new cache
	  */
	def apply[Key, Value](request: Key => Value): Cache[Key, Value] = new _Cache[Key, Value](request)
	
	/**
	  * Creates a temporarily caching cache
	  * @param threshold A time threshold after which items are removed
	  * @param request Function for requesting new values
	  * @tparam K Type of keys used
	  * @tparam V Type of values used
	  * @return A new cache
	  */
    @deprecated("Please use .expiring.after(...) instead", "v2.5")
	def expiringAfter[K, V](threshold: FiniteDuration)(request: K => V)
	                       (implicit exc: ExecutionContext) =
		ExpiringCache.after(threshold)(request)
	
	/**
	  * Creates a cache that uses weak references after a while
	  * @param threshold A time threshold after which weak references are used
	  * @param request Function for requesting new values
	  * @tparam K Type of keys used
	  * @tparam V Type of values used
	  * @return A new cache
	  */
	@deprecated("Please use .releasing.after(...) instead", "v2.5")
	def releasingAfter[K, V <: AnyRef](threshold: FiniteDuration)
	                                  (request: K => V)
	                                  (implicit exc: ExecutionContext) =
		ReleasingCache.after(threshold)(request)
		
	
	// NESTED   ---------------------------
	
	private class _Cache[Key, Value](request: Key => Value) extends Cache[Key, Value]
	{
		// ATTRIBUTES	------------------
		
		private val cachedItems: mutable.Map[Any, Value] = mutable.Map()
		
		
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
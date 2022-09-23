package utopia.flow.collection.template.caching

import utopia.flow.caching.multi.MappingCacheView
import utopia.flow.collection.template.MapLike

/**
  * Caches keep their values after requesting them
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
trait CacheLike[-Key, +Value] extends MapLike[Key, Value]
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
	def mapValuesView[O >: Value, V2](f: O => V2) =
		MappingCacheView[Key, O, V2](this)(f)
	
	/**
	  * @param f A mapping function applied for key inputs to this cache, producing a key applicable to this cache
	  * @tparam K2 Type of keys accepted by the resulting cache (map input)
	  * @tparam OK Type of keys produced by the mapping function (map output, must be a sub-type of this cache's key)
	  * @return A copy of this cache that maps the incoming keys before proposing them to this cache
	  */
	def mapKeys[K2, OK <: Key](f: K2 => OK) =
		KeyMappingCache[K2, OK, Value](this)(f)
}

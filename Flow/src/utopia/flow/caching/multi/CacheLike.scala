package utopia.flow.caching.multi

/**
  * Caches keep their values after requesting them
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
trait CacheLike[-Key, +Value]
{
	/**
	  * @return All the items currently stored in this cache
	  */
	def cachedValues: Iterable[Value]
	
	/**
	  * Finds a value for a key from this cache
	  * @param key A key
	  * @return A value for the key, the value will be cached
	  */
	def apply(key: Key): Value
	
	/**
	  * The currently cached value for specified key
	  * @param key A key
	  * @return The currently cached value for the key. None if no data is cached
	  */
	def cached(key: Key): Option[Value]
	
	/**
	  * @param key A key
	  * @return Whether there is currently cached data for the key
	  */
	def isValueCached(key: Key) = cached(key).isDefined
}

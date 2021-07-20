package utopia.flow.caching.multi

import utopia.flow.datastructure.template.MapLike

/**
  * Caches keep their values after requesting them
  * @author Mikko Hilpinen
  * @since 10.6.2019, v1.5+
  */
trait CacheLike[-Key, +Value] extends MapLike[Key, Value]
{
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
	
	/**
	  * @param key A key
	  * @return Whether there is currently cached data for the key
	  */
	def isValueCached(key: Key) = cached(key).isDefined
}

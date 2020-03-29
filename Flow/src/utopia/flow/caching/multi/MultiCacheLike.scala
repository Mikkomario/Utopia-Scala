package utopia.flow.caching.multi

import utopia.flow.caching.single.SingleCacheLike

/**
  * These caches use multiple single caches
  * @author Mikko Hilpinen
  * @since 12.6.2019, v1.5+
  */
trait MultiCacheLike[-Key, +Value, +Part <: SingleCacheLike[Value]] extends CacheLike[Key, Value]
{
	// ABSTRACT	-----------------
	
	/**
	  * Finds a cache for a specific key
	  * @param key A key
	  * @return The cache used for the specified key
	  */
	protected def cacheForKey(key: Key): Part
	
	
	// IMPLEMENTED	-------------
	
	override def apply(key: Key) = cacheForKey(key)()
	
	override def cached(key: Key) = cacheForKey(key).cached
}

package utopia.flow.view.mutable.caching

import utopia.flow.collection.immutable.caching.cache.Cache

import scala.collection.mutable

object ClearableCache
{
	// OTHER    ---------------------------
	
	/**
	  * @param f A function for generating the items to cache
	  * @tparam K Type of cache keys
	  * @tparam V Type of cache values
	  * @return A new clearable cache
	  */
	def apply[K, V](f: K => V): ClearableCache[K, V] = new _Cache[K, V](f)
	
	
	// NESTED   ---------------------------
	
	// WET WET (from Cache)
	private class _Cache[Key, Value](request: Key => Value) extends ClearableCache[Key, Value]
	{
		// ATTRIBUTES	------------------
		
		private val cachedItems: mutable.Map[Any, Value] = mutable.Map()
		
		
		// IMPLEMENTED	------------------
		
		override def cachedValues = cachedItems.values
		
		override def apply(key: Key) = cachedItems.getOrElseUpdate(key, request(key))
		override def cached(key: Key) = cachedItems.get(key)
		
		override def clear(key: Key): Unit = cachedItems -= key
		override def clear(): Unit = cachedItems.clear()
	}
}
/**
  * Common trait for cache variants which support clearing of their cached keys & values
  * @author Mikko Hilpinen
  * @since 10.01.2025, v2.5.1
  */
trait ClearableCache[-K, +V] extends Cache[K, V] with mutable.Clearable
{
	// ABSTRACT -----------------------
	
	/**
	  * Clears a specific key from this cache
	  * @param key Key that should be cleared
	  */
	def clear(key: K): Unit
}

package utopia.flow.collection.immutable.caching.cache

import scala.collection.mutable

object WeakKeysCache
{
	// OTHER    -------------------------
	
	/**
	  * @param f A function for generating cached values
	  * @tparam K Type of used keys
	  * @tparam V Type of generated values
	  * @return A cache which only weakly references the keys
	  */
	def apply[K, V](f: K => V) = new WeakKeysCache[K, V](f)
}

/**
  * A [[Cache]] implementation that weakly references the cache keys and strongly references the cached values
  * (as long as the keys remain referenced)
  * @author Mikko Hilpinen
  * @since 01.10.2024, v2.5
  */
class WeakKeysCache[K, V](f: K => V) extends Cache[K, V]
{
	// ATTRIBUTES   ----------------------------
	
	private val cache = new mutable.WeakHashMap[K, V]()
	
	
	// IMPLEMENTED  ----------------------------
	
	override def cachedValues: Iterable[V] = cache.values
	
	override def cached(key: K): Option[V] = cache.get(key)
	override def apply(key: K): V = cache.getOrElseUpdate(key, f(key))
}

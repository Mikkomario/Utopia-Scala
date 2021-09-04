package utopia.flow.caching.multi

import utopia.flow.caching.single.{ClearableSingleCacheLike, ExpiringSingleCache, SingleCacheLike}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

@deprecated("Please use MultiLazy instead", "v1.10")
object MultiCache
{
	// OTHER	--------------------------
	
	/**
	  * Creates a new multi cache
	  * @param makeCache A function for producing new caches
	  * @tparam Key The type of cache key
	  * @tparam Value The type of cache value
	  * @tparam Part The type of cache used to hold the values
	  * @return A new multi cache
	  */
	def apply[Key , Value, Part <: SingleCacheLike[Value]](makeCache: Key => Part) =
		new MultiCache[Key, Value, Part](makeCache)
	
	
	// EXTENSIONS	----------------------
	
	implicit class ClearableMultiCache[K, V, Part <: ClearableSingleCacheLike[V]](val cache: MultiCache[K, V, Part]) extends AnyVal
	{
		/**
		 * Clears all cached items
		 */
		def clear() = cache.caches.values.foreach { _.clear() }
		
		/**
		 * Creates a copy of this cache that uses content expiring
		 * @param cacheDuration Content cache time
		 * @return A new cache with expiring content
		 */
		def expiring(cacheDuration: FiniteDuration) = new MultiCache[K, V, ExpiringSingleCache[V]](k =>
			cache.makeCache(k).expiring(cacheDuration))
	}
}

/**
  * This cache uses multiple single caches for caching its data
  * @author Mikko Hilpinen
  * @since 12.6.2019, v1.5+
  */
@deprecated("Please use MultiLazy instead", "v1.10")
class MultiCache[Key, +Value, Part <: SingleCacheLike[Value]](private val makeCache: Key => Part)
	extends MultiCacheLike[Key, Value, Part]
{
	// ATTRIBUTES	-------------------
	
	private val caches: mutable.Map[Key, Part] = mutable.HashMap()
	
	
	// COMPUTED	-----------------------
	
	/**
	 * @return A list of currently cached values in this cache
	 */
	def cachedItems = caches.values.flatMap { _.cached }
	
	
	// IMPLEMENTED	-------------------
	
	override def cachedValues = caches.values.flatMap { _.cached }
	
	override protected def cacheForKey(key: Key) = caches.getOrElseUpdate(key, makeCache(key))
}

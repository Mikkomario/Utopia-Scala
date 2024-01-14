package utopia.flow.collection.template

import utopia.flow.collection.immutable.caching.cache.Cache
import utopia.flow.view.immutable.caching.Lazy

object MultiLazy
{
	// TYPES    ---------------------------------
	
	/**
	  * A cache consisting of multiple lazy containers
	  */
	type MultiLazy[-K, +V] = MultiLazyLike[K, V, Lazy[V]]
	
	
	// OTHER    ----------------------------------
	
	/**
	  * Wraps another cache
	  * @param cacheCache A cache for lazily initialized value caches
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @tparam P Type of lazy containers used
	  * @return A new cache that provides direct access to the values also
	  */
	def apply[K, V, P <: Lazy[V]](cacheCache: Cache[K, P]): MultiLazyLike[K, V, P] =
		new MultiLazyWrapper[K, V, P](cacheCache)
	
	/**
	  * Creates a new cache
	  * @param cacheForKey Function for creating a new lazy container for a key
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @tparam P Type of lazy containers used
	  * @return A new cache based on the generated lazy containers
	  */
	def apply[K, V, P <: Lazy[V]](cacheForKey: K => P): MultiLazyLike[K, V, P] =
		apply[K, V, P](Cache[K, P](cacheForKey))
	
	private class MultiLazyWrapper[-K, +V, +P <: Lazy[V]](caches: Cache[K, P])
		extends MultiLazyLike[K, V, P]
	{
		override def cachedValues = caches.cachedValues.flatMap { _.current }
		
		override def cacheFor(key: K) = caches(key)
	}
}

/**
  * A common trait for caches that consists of multiple lazily initialized value acquirers
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.10
  */
trait MultiLazyLike[-K, +V, +P <: Lazy[V]] extends Cache[K, V]
{
	// ABSTRACT ------------------------------
	
	/**
	  * @param key A key
	  * @return A cache that will hold the value for that key
	  */
	def cacheFor(key: K): P
	
	
	// IMPLEMENTED  --------------------------
	
	override def apply(key: K) = cacheFor(key).value
	
	override def cached(key: K) = cacheFor(key).current
}

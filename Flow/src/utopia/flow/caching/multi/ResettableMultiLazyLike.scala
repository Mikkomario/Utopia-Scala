package utopia.flow.caching.multi

import utopia.flow.datastructure.mutable.ResettableLazyLike

object ResettableMultiLazy
{
	// TYPES    ---------------------------------
	
	type ResettableMultiLazy[-K, +V] = ResettableMultiLazyLike[K, V, ResettableLazyLike[V]]
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param cacheForKey A function for creating a cache based on a key
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @tparam P Type of lazy containers used
	  * @return A resettable cache based on lazy containers
	  */
	def apply[K, V, P <: ResettableLazyLike[V]](cacheForKey: K => P): ResettableMultiLazyLike[K, V, P] =
		new FunctionalResettableMultiLazy[K, V, P](cacheForKey)
	
	
	// NESTED   ---------------------------------
	
	private class FunctionalResettableMultiLazy[K, V, P <: ResettableLazyLike[V]](makeCache: K => P)
		extends ResettableMultiLazyLike[K, V, P]
	{
		// ATTRIBUTES   -------------------------
		
		private var caches = Map[K, P]()
		
		
		// IMPLEMENTED  -------------------------
		
		override protected def currentCaches = caches.values
		
		override protected def findCache(key: K) = caches.get(key)
		
		override def cacheFor(key: K) = caches.getOrElse(key, {
			val newCache = makeCache(key)
			caches += key -> newCache
			newCache
		})
	}
}

/**
  * A common trait for caches that are based on a set of lazily initialized containers
  * that offer a reset functionality so that the values may be requested / generated again
  * @author Mikko Hilpinen
  * @since 16.5.2021, v1.10
  */
trait ResettableMultiLazyLike[-K, +V, +P <: ResettableLazyLike[V]] extends MultiLazyLike[K, V, P]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Currently used caches
	  */
	protected def currentCaches: Iterable[P]
	
	/**
	  * @param key A key
	  * @return An already initialized cache for that key, if there is one
	  */
	protected def findCache(key: K): Option[P]
	
	
	// IMPLEMENTED  ----------------------
	
	override def cachedValues = currentCaches.flatMap { _.current }
	
	
	// OTHER    --------------------------
	
	/**
	  * Resets a single value
	  * @param key Key for which the value is reset
	  */
	def reset(key: K) = findCache(key).foreach { _.reset() }
	
	/**
	  * Resets all values in this cache
	  */
	def resetAll() = currentCaches.foreach { _.reset() }
}

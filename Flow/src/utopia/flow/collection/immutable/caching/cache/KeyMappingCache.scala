package utopia.flow.collection.immutable.caching.cache

object KeyMappingCache
{
	/**
	  * @param wrapped Cache to wrap
	  * @param f A mapping function for incoming cache keys
	  * @tparam K Type of keys accepted by this cache
	  * @tparam KO Type of keys accepted by the original cache
	  * @tparam V Type of values returned by these caches
	  * @return A new key-mapping cache
	  */
	def apply[K, KO, V](wrapped: Cache[KO, V])(f: K => KO) = new KeyMappingCache[K, KO, V](wrapped)(f)
}

/**
  * A cache wrapper that maps applied keys before utilizing the wrapped cache
  * @author Mikko Hilpinen
  * @since 26.7.2022, v1.16
  */
class KeyMappingCache[-K, KO, +V](wrapped: Cache[KO, V])(f: K => KO) extends Cache[K, V]
{
	override def cachedValues = wrapped.cachedValues
	
	override def cached(key: K) = wrapped.cached(f(key))
	override def apply(key: K) = wrapped(f(key))
}

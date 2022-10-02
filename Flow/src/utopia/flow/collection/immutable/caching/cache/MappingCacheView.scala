package utopia.flow.collection.immutable.caching.cache

import utopia.flow.collection.template.CacheLike

object MappingCacheView
{
	/**
	  * Creates a new mapping cache view
	  * @param wrapped Cache to wrap
	  * @param f A mapping function for cache values
	  * @tparam K Type of keys accepted by these caches
	  * @tparam O Type of values in the wrapped cache
	  * @tparam R Type of mapped cache values
	  * @return A new mapping cache view
	  */
	def apply[K, O, R](wrapped: CacheLike[K, O])(f: O => R) = new MappingCacheView[K, O, R](wrapped)(f)
}

/**
  * A cache (wrapper) that maps underlying cache contents. Mapping is done for each query (i.e. is not cached).
  * @author Mikko Hilpinen
  * @since 26.7.2022, v1.16
  * @param wrapped A cache to wrap
  * @param f A mapping function applied over the wrapped cache
  * @tparam K Type of keys accepted by this cache
  * @tparam O Type of values in the wrapped cache
  * @tparam R Type of mapped cache values
  */
class MappingCacheView[-K, O, +R](wrapped: CacheLike[K, O])(f: O => R) extends CacheLike[K, R]
{
	override def cachedValues = wrapped.cachedValues.view.map(f)
	
	override def cached(key: K) = wrapped.cached(key).map(f)
	
	override def apply(key: K) = f(wrapped(key))
	
	override def isValueCached(key: K) = wrapped.isValueCached(key)
}

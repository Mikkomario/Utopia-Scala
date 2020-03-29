package utopia.flow.caching.single

/**
  * This class provides clearing functionality to any cache with wrapping
  * @author Mikko Hilpinen
  * @since 12.6.2019, v1.5+
  */
class ClearableSingleCacheWrapper[A](newCache: () => SingleCacheLike[A]) extends ClearableSingleCacheLike[A]
{
	// ATTRIBUTES	----------------------
	
	private val cacheCache = new ClearableSingleCache(newCache)
	
	
	// IMPLEMENTED	----------------------
	
	override def clear() = cacheCache.clear()
	
	override def cached = cacheCache().cached
	
	override def apply() = cacheCache()()
}

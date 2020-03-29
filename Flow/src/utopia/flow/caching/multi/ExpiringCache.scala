package utopia.flow.caching.multi

import utopia.flow.caching.single.{ExpiringSingleCache, ExpiringSingleCacheLike}

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

object ExpiringCache
{
	/**
	  * Creates a new expiring cache
	  * @param makeCache A function for making expiring single caches
	  * @tparam Key The type of key for this cache
	  * @tparam Value The type of value provided through this cache
	  * @return A new expiring cache that uses the specified caches
	  */
	def apply[Key, Value](makeCache: Key => ExpiringSingleCacheLike[Value]) = new ExpiringCache[Key, Value](makeCache)
	
	/**
	  * Creates a new expiring cache
	  * @param cacheDuration The duration before a value is considered expired
	  * @param request A function for requesting new values
	  * @tparam Key The type of key for this cache
	  * @tparam Value The type of value provided through this cache
	  * @return A new expiring cache that uses the specified function
	  */
	def apply[Key, Value](cacheDuration: FiniteDuration)(request: Key => Value) =
		new ExpiringCache[Key, Value](key => ExpiringSingleCache(cacheDuration){ request(key) })
}

/**
  * This cache re-requests its data once in a while
  * @author Mikko Hilpinen
  * @since 12.6.2019, v1.5+
  */
class ExpiringCache[Key, Value](private val makeCache: Key => ExpiringSingleCacheLike[Value])
	extends ExpiringCacheLike[Key, Value]
{
	// ATTRIBUTES	--------------------
	
	private val caches: mutable.Map[Key, ExpiringSingleCacheLike[Value]] = mutable.HashMap()
	
	
	// IMPLEMENTED	--------------------
	
	override protected def currentCaches = caches.values
	
	override protected def cacheForKey(key: Key) = caches.getOrElseUpdate(key, makeCache(key))
}

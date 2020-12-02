package utopia.flow.caching.multi

import utopia.flow.caching.single.SingleTryCache

import scala.concurrent.duration.FiniteDuration
import scala.util.Try

/**
  * This cache's requests may fail. Failures are cached for a shorter period of time
  * @author Mikko Hilpinen
  * @since 12.6.2019, v1.5+
  */
object TryCache
{
	/**
	  * Creates a new cache
	  * @param failResultDuration The duration which failed results are kept cached
	  * @param request A function for retrieving a result
	  * @tparam Key The type of key for this cache
	  * @tparam Value The result value type on success
	  * @return A new cache
	  */
	def apply[Key, Value](failResultDuration: FiniteDuration)(request: Key => Try[Value]) =
		MultiCache[Key, Try[Value], SingleTryCache[Value]] { key => SingleTryCache(failResultDuration){ request(key) } }
	
	/**
	  * Creates a new cache that expires its data
	  * @param failResultDuration The duration which failed results are kept cached
	  * @param maxResultDuration The duration which succeeded results are kept cached (should be larger than failResultDuration)
	  * @param request A function for retrieving a result
	  * @tparam Key The type of key for this cache
	  * @tparam Value The result value type on success
	  * @return A new cache
	  */
	def expiring[Key, Value](failResultDuration: FiniteDuration, maxResultDuration: FiniteDuration)(
		request: Key => Try[Value]) = ExpiringCache[Key, Try[Value]] {
		key: Key => SingleTryCache.expiring(failResultDuration, maxResultDuration){ request(key) } }
	
	/**
	  * Creates a new cache that releases its contents after a while, after which it still uses weak references
	  * @param failureReleaseAfterDuration Duration to keep failure states strongly referenced
	  * @param successReleaseAfterDuration Duration to keep success states strongly referenced
	  * @param request A function for retrieving a result
	  * @tparam Key Type of key for this cache
	  * @tparam Value Success result type
	  * @return A new cache
	  */
	def releasing[Key, Value](failureReleaseAfterDuration: FiniteDuration, successReleaseAfterDuration: FiniteDuration)
							 (request: Key => Try[Value]) =
		ReleasingCache(expiring(failureReleaseAfterDuration, successReleaseAfterDuration)(request))
}

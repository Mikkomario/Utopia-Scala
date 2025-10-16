package utopia.flow.collection.immutable.caching.cache

import scala.concurrent.ExecutionContext
import utopia.flow.time.Duration
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
	  * @param failureDuration How long failure results are cached (default = not cached)
	  * @param successDuration How long success results are cached (default = infinite)
	  * @param request         Function for acquiring a new value
	  * @param exc             Implicit execution context
	  * @tparam K Type of keys used
	  * @tparam V Type of values used
	  * @return A new cache
	  */
	def apply[K, V](failureDuration: Duration = Duration.zero, successDuration: Duration = Duration.infinite)
	               (request: K => Try[V])(implicit exc: ExecutionContext) =
		ExpiringCache(request) { (_, v) => if (v.isSuccess) successDuration else failureDuration }
	
	/**
	  * Creates a new cache
	  * @param failureReferenceDuration How long failure results are strongly referenced
	  *                                 (default = not strongly referenced)
	  * @param successReferenceDuration How long success results are strongly referenced (default = infinite)
	  * @param request                  Function for acquiring a new value
	  * @param exc                      Implicit execution context
	  * @tparam K Type of keys used
	  * @tparam V Type of values used
	  * @return A new cache
	  */
	def releasing[K, V <: AnyRef](failureReferenceDuration: Duration = Duration.zero,
	                              successReferenceDuration: Duration = Duration.infinite)
	                             (request: K => Try[V])(implicit exc: ExecutionContext) =
		ReleasingCache(request) { (_, v) => if (v.isSuccess) successReferenceDuration else failureReferenceDuration }
}

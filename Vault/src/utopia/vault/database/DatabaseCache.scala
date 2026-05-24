package utopia.vault.database

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.caching
import utopia.flow.collection.immutable.caching.cache.{Cache, ExpiringCache}
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Duration
import utopia.vault.database.value.{DbValue, LazyLookUpDbValue, RefreshingLookUpDbValue}
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.nosql.targeting.one.{AccessOneRoot, TargetingOne}
import utopia.vault.sql.Condition

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions
import scala.util.Try

object DatabaseCache
{
	// COMPUTED -------------------------
	
	/**
	 * @param cPool Implicit connection pool to use
	 * @return A new factory for constructing database caches
	 */
	def factory(implicit cPool: ConnectionPool) = DatabaseCacheFactory()
	
	
	// IMPLICIT -------------------------
	
	implicit def objectAsFactory(o: DatabaseCache.type)(implicit cPool: ConnectionPool): DatabaseCacheFactory =
		o.factory
	
	
	// OTHER    -------------------------
	
	/**
	 * Creates a new cache that accesses items based on unique row id (primary key)
	 * @param connectionPool Connection pool used when requesting new values from the database
	 * @param accessor         Database accessor used for performing actual data requests
	 * @param maxCacheDuration        Maximum duration for cached items. Infinite duration means that cached data never
	 *                                expires (default)
	 * @param maxFailureCacheDuration Maximum duration for failed requests. Infinite duration means that cached failures
	 *                                never expire (unless maxCacheDuration is specified) and will fail in the future
	 *                                as well (default).
	 * @param exc Execution context the connection pool uses (implicit)
	 * @param valueConversion Implicit conversion from specified index type to value
	 *                        (usually enough to insert utopia.flow.generic.ValueConversions._)
	 * @tparam A Type of retrieved item
	 * @tparam I Type of index used
	 * @return A new cache
	 */
	@deprecated("Deprecated for removal. Please consider using .accessingByIndex(...), if you have access to an AccessOneRoot version of 'accessor'", "v2.2")
	def forIndex[A, I](connectionPool: ConnectionPool, accessor: SingleModelAccess[A],
					   maxCacheDuration: Duration = Duration.infinite, maxFailureCacheDuration: Duration = Duration.infinite)
					  (implicit exc: ExecutionContext, valueConversion: I => Value) =
		new DatabaseCache[A, I](connectionPool, accessor, maxCacheDuration, maxFailureCacheDuration)(
			{ id => accessor.table.getPrimaryKey <=> id })
			
	
	// NESTED   ------------------------
	
	case class DatabaseCacheFactory(refreshInterval: Duration = Duration.infinite,
	                                extraUseDuration: Duration = Duration.zero)
	                               (implicit cPool: ConnectionPool)
	{
		/**
		 * @param threshold Interval between automated refreshes
		 * @param extraUse Duration after 'refreshInterval', after which a cached value may still be used (once).
		 *                 If set to a positive value, the cache is cleared after 'threshold',
		 *                 but a new value is not immediately acquired,
		 *                 unless this duration has passed since the scheduled refresh.
		 *                 Default = 0 = A new value is always acquired immediately.
		 * @return A copy of this factory applying the specified refresh interval
		 */
		def refreshingAfter(threshold: Duration, extraUse: Duration = Duration.zero) =
			copy(refreshInterval = threshold, extraUseDuration = extraUse)
		
		/**
		 * Creates a new cache that utilizes an access point and uses primary keys as keys
		 * @param accessor Root access point to use
		 * @tparam A Type of the values accessed
		 * @return A new cache that pulls items using the specified accessor
		 */
		def accessingByIndex[A](accessor: AccessOneRoot[TargetingOne[A]]) =
			accessing(accessor) { (acc, index: Int) => acc(index) }
		/**
		 * Creates a new cache that utilizes an access point
		 * @param accessor Root access point to use
		 * @param find A function that filters 'accessor' based on a key
		 * @tparam Acc Type of the access point used
		 * @tparam K Type of the keys accepted
		 * @tparam A Type of the values accessed
		 * @return A new cache that pulls items using the specified accessor + filter function
		 */
		def accessing[Acc, K, A](accessor: Acc)(find: (Acc, K) => TargetingOne[A]) =
			apply[K, A] { (key, connection) => find(accessor, key).pull(connection) }
		
		/**
		 * @param f A function used for pulling values from the DB.
		 *          Receives:
		 *          1. Targeted key
		 *          1. DB connection
		 * @tparam K Type of accepted keys
		 * @tparam V Type of pulled values
		 * @return A new cache that uses the specified function to pull data from the DB
		 */
		def apply[K, V](f: (K, Connection) => V) = {
			// Determines the implementation based on the specified values
			val makeValue = {
				if (refreshInterval.isPositive) {
					// Case: Refreshing
					if (refreshInterval.isFinite)
						{ key: K => RefreshingLookUpDbValue(refreshInterval, extraUseDuration) { f(key, _) } }
					// Case: Caching
					else
						{ key: K => LazyLookUpDbValue { f(key, _) } }
				}
				// Case: View only
				else
					{ key: K => DbValue { f(key, _) } }
			}
			Cache(makeValue)
		}
	}
}

/**
 * Used for accessing database data. Caches retrieved data to optimize further requests
 * @author Mikko Hilpinen
 * @since 9.1.2020, v1.4
 * @param connectionPool Connection pool used when requesting new values from the database
 * @param accessor Database accessor used for performing actual data requests
 * @param maxCacheDuration Maximum duration for cached items. Infinite duration means that cached data never
 *                         expires (default)
 * @param maxFailureCacheDuration Maximum duration for failed requests. Infinite duration means that cached failures
 *                                never expire (unless maxCacheDuration is specified) and will fail in the future
 *                                as well (default).
 * @param keyToCondition A function for transforming provided keys to database search conditions
 * @param exc Execution context the connection pool uses (implicit)
 */
@deprecated("This CLASS is deprecated for removal. Note that the companion object will be preserved.", "v2.2")
class DatabaseCache[A, Key](connectionPool: ConnectionPool, accessor: SingleModelAccess[A],
							maxCacheDuration: Duration = Duration.infinite,
							maxFailureCacheDuration: Duration = Duration.infinite)
						   (keyToCondition: Key => Condition)
						   (implicit exc: ExecutionContext)
	extends Cache[Key, Try[A]]
{
	// ATTRIBUTES	-----------------------
	
	private val cache = {
		// Creates a different type of cache based on specified parameters
		maxCacheDuration.ifFinite match {
			case Some(maxTime) =>
				maxFailureCacheDuration.ifFinite match {
					case Some(maxFailTime) => caching.cache.TryCache(maxFailTime, maxTime)(request)
					case None => ExpiringCache.after(maxTime)(request)
				}
			case None =>
				maxFailureCacheDuration.ifFinite match {
					case Some(maxFailTime) => caching.cache.TryCache(maxFailTime)(request)
					case None => Cache(request)
				}
		}
	}
	
	
	// IMPLEMENTED	----------------------
	
	override def cachedValues = cache.cachedValues
	
	override def apply(key: Key) = cache(key)
	override def cached(key: Key) = cache.cached(key)
	
	
	// OTHER	--------------------------
	
	private def request(key: Key) = {
		connectionPool.tryWith { implicit connection =>
			val condition = keyToCondition(key)
			accessor.find(condition).toTry(
				new NoSuchElementException(s"No value for key '$key'. Using search condition: $condition"))
		}.flatten
	}
}
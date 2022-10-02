package utopia.vault.database

import utopia.flow.collection.immutable.caching
import utopia.flow.collection.immutable.caching.cache.{Cache, ExpiringCache}
import utopia.flow.collection.template.CacheLike
import utopia.flow.generic.model.immutable.Value
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.vault.nosql.access.single.model.SingleModelAccess
import utopia.vault.sql.Condition

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Try

object DatabaseCache
{
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
	def forIndex[A, I](connectionPool: ConnectionPool, accessor: SingleModelAccess[A],
					   maxCacheDuration: Duration = Duration.Inf, maxFailureCacheDuration: Duration = Duration.Inf)
					  (implicit exc: ExecutionContext, valueConversion: I => Value) = new DatabaseCache[A, I](
		connectionPool, accessor, maxCacheDuration, maxFailureCacheDuration)({ id => accessor.table.primaryColumn.get <=> id })
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
class DatabaseCache[A, Key](connectionPool: ConnectionPool, accessor: SingleModelAccess[A],
							maxCacheDuration: Duration = Duration.Inf, maxFailureCacheDuration: Duration = Duration.Inf)
						   (keyToCondition: Key => Condition)(implicit exc: ExecutionContext) extends CacheLike[Key, Try[A]]
{
	// ATTRIBUTES	-----------------------
	
	private val cache =
	{
		// Creates a different type of cache based on specified parameters
		maxCacheDuration.finite match
		{
			case Some(maxTime) =>
				maxFailureCacheDuration.finite match
				{
					case Some(maxFailTime) => caching.cache.TryCache(maxFailTime, maxTime)(request)
					case None => ExpiringCache.after(maxTime)(request)
				}
			case None =>
				maxFailureCacheDuration.finite match
				{
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
	
	private def request(key: Key) =
	{
		connectionPool.tryWith { implicit connection =>
			val condition = keyToCondition(key)
			accessor.find(condition).toTry(
				new NoSuchElementException(s"No value for key '$key'. Using search condition: $condition"))
		}.flatten
	}
}
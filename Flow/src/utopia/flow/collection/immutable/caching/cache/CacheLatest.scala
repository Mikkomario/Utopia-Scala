package utopia.flow.collection.immutable.caching.cache

import scala.annotation.unchecked.uncheckedVariance

object CacheLatest
{
	/**
	  * Creates a new cache that only caches the latest requested value
	  * @param f A function for generating values for requested keys
	  * @tparam K Type of keys used
	  * @tparam V Type of values returned
	  * @return A new cache
	  */
	def apply[K, V](f: K => V) = new CacheLatest[K, V](f)
}

/**
  * Caches only the latest generated value.
  * Useful in situations where the range of possible input is large,
  * and the chances of consecutively requesting the same value high.
  * @author Mikko Hilpinen
  * @since 14/01/2024, v2.3
  */
class CacheLatest[-K, +V](f: K => V) extends Cache[K, V]
{
	// ATTRIBUTES   ----------------------
	
	private var cached: Option[(K @uncheckedVariance, V @uncheckedVariance)] = None
	
	
	// IMPLEMENTED  ----------------------
	
	override def cachedValues: Iterable[V] = cached.map { _._2 }
	
	override def cached(key: K): Option[V] = cached.flatMap {
		case (k, v) if k == key => Some(v)
		case _ => None
	}
	override def apply(key: K): V = cached.filter { _._1 == key } match {
		case Some((_, v)) => v
		case None =>
			val v = f(key)
			cached = Some(key -> v)
			v
	}
}

package utopia.flow.collection.immutable.caching.cache

import utopia.flow.collection.CollectionExtensions._
import scala.collection.mutable
import scala.ref.WeakReference

object WeakCache
{
	// OTHER    --------------------------
	
	/**
	  * @param f A function for calculating a value for a key
	  * @tparam K Type of used keys
	  * @tparam V Type of cached values
	  * @return A new weakly referencing cache
	  */
	def apply[K, V <: AnyRef](f: K => V) = new WeakCache[K, V](f)
	
	/**
	  * @param f A function for calculating a value for a key
	  * @tparam K Type of used keys
	  * @tparam V Type of cached values
	  * @return A new cache that weakly references the keys but strongly references the values
	  *         (as long as the keys remain referenced)
	  */
	def weakKeys[K, V](f: K => V) = WeakKeysCache(f)
	/**
	  * @param f A function for calculating a value for a key
	  * @tparam K Type of used keys
	  * @tparam V Type of cached values
	  * @return A new cache that weakly references the values but strongly references the keys
	  */
	def weakValues[K, V <: AnyRef](f: K => V) = WeakValuesCache[K, V](f)
}

/**
  * A [[Cache]] implementation that weakly references both keys and values
  * @author Mikko Hilpinen
  * @since 01.10.2024, v2.5
  * @see [[WeakValuesCache]]
  */
class WeakCache[K, V <: AnyRef](f: K => V) extends Cache[K, V]
{
	// ATTRIBUTES   -----------------------
	
	private val cache = new mutable.WeakHashMap[K, WeakReference[V]]()
	
	
	// IMPLEMENTED  -----------------------
	
	override def cachedValues: Iterable[V] = cache.valuesIterator.flatMap { _.get }.caching
	
	override def cached(key: K): Option[V] = cache.get(key).flatMap { _.get }
	override def apply(key: K): V = cached(key).getOrElse {
		val value = f(key)
		cache += (key -> WeakReference(value))
		value
	}
}

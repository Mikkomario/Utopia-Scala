package utopia.flow.collection.immutable.caching.cache

import scala.annotation.unchecked.uncheckedVariance
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
  * @see [[WeakValuesCache]], if you only want the values to be weakly referenced
  */
class WeakCache[-K, +V <: AnyRef](f: K => V) extends WeakValuesCache[K, V]
{
	// ATTRIBUTES   -----------------------
	
	override protected val refs: mutable.WeakHashMap[K, WeakReference[V]] @uncheckedVariance =
		new mutable.WeakHashMap[K, WeakReference[V]]()
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def cachedValueRefsIterator: Iterator[WeakReference[V]] = refs.valuesIterator
	
	override protected def cachedRefFor(key: K): Option[WeakReference[V]] = refs.get(key)
	
	override protected def request(key: K): V = f(key)
}

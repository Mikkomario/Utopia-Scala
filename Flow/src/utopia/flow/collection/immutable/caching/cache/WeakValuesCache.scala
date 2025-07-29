package utopia.flow.collection.immutable.caching.cache

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.mutable.caching.ClearableCache

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable
import scala.ref.WeakReference

object WeakValuesCache
{
	// OTHER    ------------------------
	
	/**
	  * @param request A function for retrieving a new value when one is required
	  * @tparam K Type of cache keys
	  * @tparam V Type of cache values
	  * @return A new cache that weakly references its values, but strongly references its keys
	  */
	def apply[K, V <: AnyRef](request: K => V): WeakValuesCache[K, V] = new _WeakValuesCache[K, V](request)
	
	
	// NESTED   ------------------------
	
	private class _WeakValuesCache[K, V <: AnyRef](f: K => V) extends WeakValuesCache[K, V]
	{
		// ATTRIBUTES   ----------------
		
		override protected val refs: mutable.Map[K, WeakReference[V]] = mutable.Map()
		
		
		// IMPLEMENTED  ----------------
		
		override protected def cachedValueRefsIterator: Iterator[WeakReference[V]] = refs.valuesIterator
		
		override protected def cachedRefFor(key: K): Option[WeakReference[V]] = refs.get(key)
		
		override protected def request(key: K): V = f(key)
	}
}

/**
  * This cache only weakly references its values.
  * @author Mikko Hilpinen
  * @since 11.11.2020, v1.9
  * @tparam Key Type of keys used
  * @tparam Value Type of values stored (weakly referenced)
  * @see [[WeakCache]] if you want the keys to be weakly referenced, also
  */
trait WeakValuesCache[-Key, +Value <: AnyRef] extends ClearableCache[Key, Value]
{
	// ABSTRACT -------------------
	
	/**
	  * @return A mutable collection containing the wrapped references.
	  *         Only used in a manner where keys are contravariant and values are covariant.
	  */
	protected def refs: mutable.Growable[(Key @uncheckedVariance, WeakReference[Value @uncheckedVariance])]
		with mutable.Shrinkable[Key] with Iterable[(Key @uncheckedVariance, WeakReference[Value])]
	
	/**
	  * @return An iterator that yields all cached value references
	  */
	protected def cachedValueRefsIterator: Iterator[WeakReference[Value]]
	/**
	  * @param key Targeted key
	  * @return A cached reference matching that key. None if no reference has been cached.
	  */
	protected def cachedRefFor(key: Key): Option[WeakReference[Value]]
	
	/**
	  * @param key A key for which a new value needs to be acquired
	  * @return A value matching that key
	  */
	protected def request(key: Key): Value
	
	
	// IMPLEMENTED	---------------
	
	override def cachedValues = cachedValueRefsIterator.flatMap { _.get }.caching
	
	override def cached(key: Key) = cachedRefFor(key).flatMap { _.get }
	override def apply(key: Key) = {
		// Tries to use a cached or a weakly cached value
		cached(key).getOrElse {
			// But may have to request a new value
			val newValue = request(key)
			refs += (key -> WeakReference(newValue))
			newValue
		}
	}
	
	override def clear(key: Key): Unit = refs -= key
	override def clear(): Unit = refs.clear()
	
	
	// OTHER    ------------------
	
	/**
	  * Removes all keys where the value has been garbage-collected.
	  */
	def prune(): Unit = refs --= refs.iterator.filter { _._2.get.isEmpty }.map { _._1 }
}


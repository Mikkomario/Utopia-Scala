package utopia.flow.collection.immutable.caching.cache

import utopia.flow.collection.CollectionExtensions._
import scala.collection.immutable.HashMap
import scala.ref.WeakReference

object WeakValuesCache
{
	/**
	  * @param request A function for retrieving a new value when one is required
	  * @tparam K Type of cache keys
	  * @tparam V Type of cache values
	  * @return A new cache
	  */
	def apply[K, V <: AnyRef](request: K => V) = new WeakValuesCache[K, V](request)
}

/**
  * This cache only weakly references its values.
  * However, the keys are strongly referenced.
  * @author Mikko Hilpinen
  * @since 11.11.2020, v1.9
  * @see [[WeakCache]]
  */
class WeakValuesCache[Key, Value <: AnyRef](request: Key => Value) extends Cache[Key, Value]
{
	// ATTRIBUTES	---------------
	
	private var weakRefs: Map[Key, WeakReference[Value]] = HashMap()
	
	
	// IMPLEMENTED	---------------
	
	override def cachedValues = weakRefs.valuesIterator.flatMap { _.get }.caching
	
	override def cached(key: Key) = weakRefs.get(key).flatMap { _.get }
	
	override def apply(key: Key) = {
		// Tries to use a cached or a weakly cached value
		cached(key).getOrElse {
			// But may have to request a new value
			val newValue = request(key)
			weakRefs += (key -> WeakReference(newValue))
			newValue
		}
	}
}


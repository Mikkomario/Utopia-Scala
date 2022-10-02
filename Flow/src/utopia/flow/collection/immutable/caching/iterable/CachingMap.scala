package utopia.flow.collection.immutable.caching.iterable

import utopia.flow.collection.mutable.builder.CompoundingMapBuilder
import utopia.flow.collection.mutable.iterator.PollableOnce
import utopia.flow.view.immutable.View

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.MapOps.WithFilter
import scala.collection.immutable.{MapOps, VectorBuilder}
import scala.collection.{IterableOps, MapFactory, mutable}

object CachingMap extends MapFactory[CachingMap]
{
	// IMPLEMENTED  ------------------------
	
	override def from[K, V](source: IterableOnce[(K, V)]) = source match {
		case c: CachingMap[K, V] => c
		case map: Map[K, V] => initialized(map)
		case c => new CachingMap[K, V](c.iterator)
	}
	
	override def empty[K, V]: CachingMap[K, V] = new CachingMap(Iterator.empty)
	
	override def newBuilder[K, V] =
		new VectorBuilder[(K, V)]().mapResult { v => new CachingMap[K, V](v.iterator) }
	
	
	// OTHER    ----------------------------
	
	/**
	  * Creates a new, already initialized map by wrapping another map
	  * @param map A map to wrap
	  * @tparam K Type of map keys
	  * @tparam V Type of map values
	  * @return A new caching map (wrapper)
	  */
	def initialized[K, V](map: Map[K, V]) = new CachingMap[K, V](Iterator.empty, map)
	
	/**
	  * Wraps an iterator into a caching map. Please note that this doesn't consume the iterator all at once,
	  * only lazily. Modifications to that iterator from other sources will affect the resulting collection
	  * (and are not recommended).
	  * @param source A source iterator
	  * @param preCached Already cached items that don't need to be lazily initialized (default = empty)
	  * @tparam K Type of keys returned by that iterator
	  * @tparam V Type of values returned by that iterator
	  * @return A caching map based on that iterator
	  */
	def apply[K, V](source: IterableOnce[(K, V)], preCached: Map[K, V] = Map[K, V]()) = {
		if (preCached.isEmpty)
			from[K, V](source)
		else
			new CachingMap[K, V](source.iterator, preCached)
	}
	def apply[K, V](items: View[(K, V)]*) = new CachingMap[K, V](items.iterator.map { _.value })
	
	/**
	  * @param item A single item (lazily initialized / call-by-name)
	  * @tparam K Type of that item's key
	  * @tparam V Type of that item's value
	  * @return A caching map that will contain that item only
	  */
	def single[K, V](item: => (K, V)) = new CachingMap[K, V](PollableOnce(item))
	
	def fromFunctions[K, V](items: (() => (K, V))*): CachingMap[K, V] = new CachingMap[K, V](items.iterator.map { _() })
}

/**
  * A Map implementation that initializes items only when necessary.
  * Please note, that if the source iterator includes a key twice, this map may return different values at different
  * points of caching.
  * @author Mikko Hilpinen
  * @since 29.9.2022, v2.0
  */
class CachingMap[K, +V] private(source: Iterator[(K, V)] = Iterator.empty[(K, V)],
                                preCached: Map[K, V] = Map[K, V]())
	extends AbstractCachingIterable[(K, V), CompoundingMapBuilder[K, V @uncheckedVariance], Map[K, V]](
		source, new CompoundingMapBuilder[K, V](preCached))
		with Map[K, V] with MapOps[K, V, CachingMap, CachingMap[K, V]]
		with IterableOps[(K, V), CachingSeq, CachingMap[K, V]]
{
	// IMPLEMENTED  --------------------------
	
	override def get(key: K) = builder.get(key).orElse { cacheIterator.find { _._1 == key }.map { _._2 } }
	
	override def removed(key: K) = {
		// Fully caches this map when removing keys
		if (builder.contains(key) || cacheRemaining().exists { _._1 == key })
			CachingMap.initialized(fullyCached() - key)
		else
			this
	}
	override def updated[V1 >: V](key: K, value: V1) = {
		// Fully caches this map when updating
		if (builder.contains(key) || cacheRemaining().exists { _._1 == key })
			CachingMap.initialized(fullyCached().updated(key, value))
		else
			new CachingMap[K, V1](Iterator.single(key -> value), fullyCached())
	}
	
	override def empty = CachingMap.empty[K, V]
	override def iterableFactory = CachingSeq
	override def mapFactory = CachingMap
	
	override protected def fromSpecific(coll: IterableOnce[(K, V @uncheckedVariance)]) =
		CachingMap.from(coll)
	override protected def newSpecificBuilder: mutable.Builder[(K, V @uncheckedVariance), CachingMap[K, V]] =
		CachingMap.newBuilder
	
	override def withFilter(p: ((K, V)) => Boolean) = new FilteredView(p)
	
	override def removedAll(keys: IterableOnce[K]) = {
		if (isFullyCached)
			CachingMap.initialized(fullyCached() -- keys)
		else {
			val k = keys.iterator.toSet
			if (k.isEmpty)
				this
			else
				new CachingMap[K, V](iterator.filterNot { case (key, _) => k.contains(key) })
		}
	}
	
	override def keys = if (isFullyCached) builder.currentState.keys else map { _._1 }
	override def values = if (isFullyCached) builder.currentState.values else map { _._2 }
	
	override def contains(key: K) = builder.contains(key) || cacheIterator.exists { _._1 == key }
	override def isDefinedAt(key: K) = contains(key)
	
	override def concat[V2 >: V](suffix: IterableOnce[(K, V2)]) = {
		if (isFullyCached)
			new CachingMap[K, V2](suffix.iterator, fullyCached())
		else
			new CachingMap[K, V2](iterator ++ suffix)
	}
	
	override def take(n: Int) = {
		if (n <= 0)
			CachingMap.empty[K, V]
		else if (isFullyCached && sizeCompare(n) <= 0)
			this
		else if (builder.sizeCompare(n) >= 0)
			CachingMap.initialized(builder.currentState.take(n))
		else
			new CachingMap[K, V](iterator.take(n))
	}
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param key A key
	  * @return An already cached item in this map, matching that key.
	  *         None if no item in this map contains that key, or if that key hasn't yet been cached.
	  */
	def getCached(key: K) = builder.get(key)
	
	
	// NESTED   -----------------------------
	
	class FilteredView(p: ((K, V)) => Boolean) extends WithFilter[K, V, CachingSeq, CachingMap](CachingMap.this, p)
}

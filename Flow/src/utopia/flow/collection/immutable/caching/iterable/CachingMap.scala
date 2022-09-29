package utopia.flow.collection.immutable.caching.iterable

import utopia.flow.collection.mutable.builder.CompoundingMapBuilder
import utopia.flow.collection.mutable.iterator.PollableOnce
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.view.immutable.View

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.{MapOps, VectorBuilder}
import scala.collection.{IterableFactory, MapFactory, SeqFactory, mutable}

object CachingMap extends MapFactory[CachingMap]
{
	// IMPLEMENTED  ------------------------
	
	override def from[K, V](source: IterableOnce[(K, V)]) = source match {
		case c: CachingMap[K, V] => c
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
	  * @tparam K Type of keys returned by that iterator
	  * @tparam V Type of values returned by that iterator
	  * @return A caching map based on that iterator
	  */
	def apply[K, V](source: IterableOnce[(K, V)]) = new CachingMap[K, V](source.iterator)
	def apply[K, V](items: View[(K, V)]*) = new CachingMap[K, V](items.iterator.map { _.value })
	
	/**
	  * @param item A single item (lazily initialized / call-by-name)
	  * @tparam K Type of that item's key
	  * @tparam V Type of that item's value
	  * @return A caching map that will contain that item only
	  */
	def single[K, V](item: => (K, V)) = new CachingMap[K, V](PollableOnce(item))
	
	def fromFunctions[K, V](items: (() => (K, V))*): CachingMap[K, V] = apply(items.map { _() })
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
{
	// IMPLEMENTED  --------------------------
	
	override def get(key: K) = builder.get(key).orElse { cacheIterator.find { _._1 == key }.map { _._2 } }
	
	override def removed(key: K) = {
		if (builder.contains(key))
			new CachingMap[K, V](cacheIterator, builder.currentState)
		else if (isFullyCached)
			this
		else
			new CachingMap[K, V](cacheIterator.filterNot { _._1 == key }, builder.currentState)
	}
	override def updated[V1 >: V](key: K, value: V1) = {
		if (builder.contains(key))
			new CachingMap[K, V1](cacheIterator, builder.currentState.updated(key, value))
		else if (isFullyCached)
			CachingMap.initialized(builder.currentState.updated(key, value))
		else
			new CachingMap[K, V1](Iterator.single(key -> value) ++ cacheIterator.filterNot { _._1 == key },
				builder.currentState)
	}
	
	override def empty = CachingMap.empty[K, V]
	override def iterableFactory = CachingSeq
	override def mapFactory = CachingMap
	
	override protected def fromSpecific(coll: IterableOnce[(K, V @uncheckedVariance)]) =
		CachingMap.from(coll)
	override protected def newSpecificBuilder: mutable.Builder[(K, V @uncheckedVariance), CachingMap[K, V]] =
		CachingMap.newBuilder
	
	override def removedAll(keys: IterableOnce[K]) = {
		if (isFullyCached)
			CachingMap.initialized(builder.currentState -- keys)
		else {
			val k = keys.iterator.toSet
			new CachingMap[K, V](cacheIterator.filterNot { case (key, _) => k.contains(key) },
				builder.currentState -- k)
		}
	}
	
	override def keys = if (isFullyCached) builder.currentState.keys else map { _._1 }
	override def values = if (isFullyCached) builder.currentState.values else map { _._2 }
	
	override def contains(key: K) = builder.contains(key) || cacheIterator.exists { _._1 == key }
	override def isDefinedAt(key: K) = contains(key)
	
	override def concat[V2 >: V](suffix: IterableOnce[(K, V2)]) =
		new CachingMap[K, V2](cacheIterator ++ suffix, builder.currentState)
	
	
	// OTHER    ------------------------------
	
	/**
	  * @param key A key
	  * @return An already cached item in this map, matching that key.
	  *         None if no item in this map contains that key, or if that key hasn't yet been cached.
	  */
	def getCached(key: K) = builder.get(key)
}

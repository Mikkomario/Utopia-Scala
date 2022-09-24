package utopia.flow.collection.immutable

import utopia.flow.collection.template.MapLike
import utopia.flow.generic.model.immutable.{Constant, Model, Value}
import utopia.flow.generic.model.mutable.AnyType
import utopia.flow.generic.ValueConvertible
import utopia.flow.generic.model.template.{ModelConvertible, ValueConvertible}
import utopia.flow.util.CollectionExtensions._

object DeepMap
{
	/**
	  * Creates a new empty deep map
	  * @tparam K Type of stored keys
	  * @tparam V Type of stored values
	  * @return A new empty map
	  */
	def empty[K, V] = new DeepMap[K, V](Map())
	
	/**
	  * Creates a new deep map
	  * @param items Items to store in this map where each item is a pair
	  *              consisting of item path and the value at the end of that path
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @return A new deep map
	  */
	def apply[K, V](items: IterableOnce[(IterableOnce[K], V)]): DeepMap[K, V] = {
		// Divides the items into direct values and nested values
		val (deep, direct) = items.iterator
			.map { case (path, value) => path.iterator -> value }
			.filter { _._1.hasNext }
			.divideWith { case (path, value) =>
				val key = path.next()
				if (path.hasNext)
					Left((key, path, value))
				else
					Right(key -> Right(value))
			}
		// Groups nested values based on keys
		val deepMap = deep.groupMap { _._1 } { case (_, path, value) => path -> value }
		// Forms a new deep map from the collected data (nested values are recursively converted to maps)
		new DeepMap[K, V](direct.toMap ++ deepMap.view.mapValues { values => Left(DeepMap(values)) })
	}
	/**
	  * Creates a new deep map
	  * @param items Items to store in this map where each item is a pair
	  *              consisting of item path and the value at the end of that path
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @return A new deep map
	  */
	def apply[K, V](items: (IterableOnce[K], V)*): DeepMap[K, V] = apply(items)
	/**
	  * Creates a new deep map with a single key / value
	  * @param path Path to the value
	  * @param value Value at that path
	  * @tparam K Type of keys to use
	  * @tparam V Type of values to store
	  * @return A new deep map
	  */
	def apply[K, V](path: IterableOnce[K], value: V): DeepMap[K, V] = _apply(path.iterator, value)
	
	/**
	  * Creates a new map
	  * @param key Key to use
	  * @param value Value to store
	  * @tparam K Type of key used
	  * @tparam V Type of values stored
	  * @return A new map
	  */
	def flat[K, V](key: K, value: V) = new DeepMap(Map(key -> Right(value)))
	/**
	  * Creates a new map
	  * @param items Items to store in this map (key value -pairs)
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @return A new map
	  */
	def flat[K, V](items: IterableOnce[(K, V)]) =
		new DeepMap[K, V](items.iterator.map { case (k, v) => k -> Right(v) }.toMap)
	/**
	  * Creates a new map
	  * @param items Items to store in this map (key value -pairs)
	  * @tparam K Type of keys used
	  * @tparam V Type of values stored
	  * @return A new map
	  */
	def flat[K, V](items: (K, V)*): DeepMap[K, V] = flat(items)
	
	/**
	  * Creates a new map containing another map at the end of a nested path
	  * @param path Path to the nested map
	  * @param map Map to store
	  * @tparam K Types of keys on the path
	  * @tparam V Types of values in the nested map
	  * @return A deep map containing that nested map
	  */
	def nested[K, V](path: IterableOnce[K], map: DeepMap[K, V]) = _nested(path.iterator, map)
	/**
	  * Creates a new map containing another map at the end of a nested path
	  * @param path Path to the nested map
	  * @param map Map to store
	  * @tparam K Types of keys on the path
	  * @tparam V Types of values in the nested map
	  * @return A deep map containing that nested map
	  */
	def nested[K, V](path: IterableOnce[K], map: IterableOnce[(K, V)]): DeepMap[K, V] = nested(path, flat(map))
	
	private def _apply[K, V](iter: Iterator[K], value: V): DeepMap[K, V] = {
		if (iter.hasNext) {
			val key = iter.next()
			if (iter.hasNext)
				new DeepMap(Map(key -> Left(_apply(iter, value))))
			else
				flat(key, value)
		}
		else
			empty
	}
	private def _nested[K, V](iter: Iterator[K], map: DeepMap[K, V]): DeepMap[K, V] = {
		if (iter.hasNext) {
			val key = iter.next()
			if (iter.hasNext)
				new DeepMap(Map(key -> Left(_nested(iter, map))))
			else
				new DeepMap(Map(key -> Left(map)))
		}
		else
			map
	}
}

/**
  * A map where keys are paths which may contain one or more items
  * @author Mikko Hilpinen
  * @since 25.12.2021, v1.14.1
  */
case class DeepMap[K, +V] private(private val wrapped: Map[K, Either[DeepMap[K, V], V]])
	extends MapLike[Iterable[K], V] with Iterable[(Vector[K], V)] with ModelConvertible
{
	// COMPUTED -------------------------------
	
	/**
	  * @return A copy of this map where all nested values are discarded and only direct values are present
	  */
	def flat = wrapped.flatMap { case (key, value) => value.toOption.map { key -> _ } }
	/**
	  * @return A copy of this map all direct values are discarded and only nested values are present
	  */
	def deep = wrapped.flatMap { case (key, value) => value.leftOption.map { key -> _ } }
	
	
	// IMPLEMENTED  ---------------------------
	
	override def toModel: Model = {
		val constants = wrapped.map { case (key, value) =>
			val v = value match {
				case Left(map) => map.toValue
				case Right(value) =>
					value match {
						case v: Value => v
						case v: ValueConvertible => v.toValue
						// TODO: Could be more advanced / precise
						case v => Value(Some(v), AnyType)
					}
			}
			Constant(key.toString, v)
		}
		Model.withConstants(constants)
	}
	
	override def iterator: Iterator[(Vector[K], V)] = wrapped.iterator.flatMap { case (key, value) =>
		value match {
			case Right(direct) => Some(Vector(key) -> direct)
			case Left(nested) => nested.iterator.map { case (path, value) => (key +: path) -> value }
		}
	}
	
	override def size: Int = wrapped.valuesIterator.map {
		case Right(_) => 1
		case Left(nested) => nested.size
	}.sum
	
	@throws[NoSuchElementException]("If there is no value on that path")
	override def apply(path: Iterable[K]) =
		get(path).getOrElse { throw new NoSuchElementException(s"No value for [${path.mkString(", ")}]") }
	
	
	// OTHER    -------------------------------
	
	@throws[NoSuchElementException]("If there is no value on that path")
	def apply(keys: K*): V = apply(keys)
	
	/**
	  * @param path Targeted path
	  * @return A nested map at the end of that path (or an empty map)
	  */
	def nested(path: IterableOnce[K]) = getNested(path).getOrElse(DeepMap.empty)
	/**
	  * @param key A key
	  * @return A nested map behind that key (or an empty map)
	  */
	def nested(key: K) = getNested(key).getOrElse(DeepMap.empty)
	/**
	  * @param first First path part
	  * @param second Second path part
	  * @param more More path parts
	  * @return A nested map at the end of that path (or an empty map)
	  */
	def nested(first: K, second: K, more: K*): DeepMap[K, V] = nested(Vector(first, second) ++ more)
	
	/**
	  * @param path A path to look up
	  * @return Value found with that path. None if no value was found.
	  */
	def get(path: IterableOnce[K]) = _apply(path.iterator)
	/**
	  * @param key A key
	  * @return A direct value in this map for that key. None if there was no direct value.
	  */
	def get(key: K) = wrapped.get(key).flatMap { _.toOption }
	/**
	  * @param key1 First level key
	  * @param key2 Second level key
	  * @param more Deeper keys
	  * @return A value from that path. None if no value was found
	  */
	def get(key1: K, key2: K, more: K*): Option[V] = get(Vector(key1, key2) ++ more)
	
	/**
	  * @param key A key
	  * @return Nested map from that key. An empty map if that key was not defined.
	  */
	def /(key: K): DeepMap[K, V] = wrapped.get(key) match {
		case Some(found) =>
			found match {
				case Right(_) => DeepMap.empty
				case Left(map) => map
			}
		case None => DeepMap.empty
	}
	
	/**
	  * @param key A key
	  * @return Nested map for that key. None if there was no nested map for that key.
	  */
	def getNested(key: K): Option[DeepMap[K, V]] = wrapped.get(key).flatMap { _.leftOption }
	/**
	  * @param path Path to target
	  * @return A nested map at the end of that path. None if that path didn't point to a nested map
	  */
	def getNested(path: IterableOnce[K]): Option[DeepMap[K, V]] = _getNested(path.iterator)
	/**
	  * @param first First path part
	  * @param second Second path part
	  * @param more More path parts
	  * @return A nested map at the end of that path. None if that path didn't point to a nested map
	  */
	def getNested(first: K, second: K, more: K*): Option[DeepMap[K, V]] = getNested(Vector(first, second) ++ more)
	
	/**
	  * @param key A key
	  * @return Whether this map contains a <b>direct value</b> for that key
	  */
	def contains(key: K) = wrapped.get(key).exists { _.isRight }
	/**
	  * @param path A path
	  * @return Whether this map contains a value on that path
	  */
	def contains(path: IterableOnce[K]) = _contains(path.iterator)
	
	/**
	  * @param pair A path value pair
	  * @tparam V2 Type of value stored
	  * @return A copy of this map with that value added
	  */
	def +[V2 >: V](pair: (IterableOnce[K], V2)) = _plus(pair._1.iterator, pair._2)
	
	/**
	  * @param key A key to use
	  * @param value A direct value to store
	  * @tparam V2 Type of value
	  * @return A copy of this map with that direct value added
	  */
	def withValue[V2 >: V](key: K, value: V2) = copy(wrapped + (key -> Right(value)))
	/**
	  * @param key A key to use
	  * @param map A nested map to store
	  * @tparam V2 Type of values in that map
	  * @return A copy of this map with the specified map stored for that key (merged if necessary)
	  */
	def withMap[V2 >: V](key: K, map: DeepMap[K, V2]) = {
		val newMap: DeepMap[K, V2] = getNested(key) match {
			case Some(existing) => existing ++ map
			case None => map
		}
		copy(wrapped + (key -> Left(newMap)))
	}
	/**
	  * @param key A key to use
	  * @param map A nested map to store
	  * @tparam V2 Type of values in that map
	  * @return A copy of this map with the specified map stored for that key (merged if necessary)
	  */
	def withMap[V2 >: V](key: K, map: IterableOnce[(K, V2)]): DeepMap[K, V2] = withMap(key, DeepMap.flat(map))
	
	/**
	  * @param pair A key map pair
	  * @tparam V2 Type of values in that map
	  * @return A copy of this map with that map stored to specified key (merged if necessary)
	  */
	def ++[V2 >: V](pair: (IterableOnce[K], DeepMap[K, V2])) = _plusMap(pair._1.iterator, pair._2)
	/**
	  * @param values Path value pairs to add
	  * @tparam V2 Type of values to add
	  * @return A copy of this map with those values added to those paths (merged if possible)
	  */
	def ++[V2 >: V](values: IterableOnce[(IterableOnce[K], V2)]) =
		_plusMany(values.iterator.map { case (c, v) => c.iterator -> v })
	/**
	  * @param other Another deep map
	  * @tparam V2 Type of values stored in that map
	  * @return A combination of these maps - nested maps are merged where possible
	  */
	def ++[V2 >: V](other: DeepMap[K, V2]): DeepMap[K, V2] = {
		val (deepUpdates, directUpdates) = other.wrapped
			.divideWith { case (key, value) => value.mapBoth { key -> _ } { v => key -> Right(v) } }
		val mergedDeepUpdates = deepUpdates.map { case (key, map) =>
			val newMap = getNested(key) match {
				case Some(existing) => existing ++ map
				case None => map
			}
			key -> Left(newMap)
		}
		copy(wrapped ++ directUpdates ++ mergedDeepUpdates)
	}
	/**
	  * @param key A key to remove
	  * @return This map without that key present
	  */
	def -(key: K) = copy(wrapped - key)
	/**
	  * @param path A path to remove
	  * @return This map without that path (end) present
	  */
	def -(path: IterableOnce[K]) = _minus(path.iterator)
	
	private def _apply(iter: Iterator[K]): Option[V] = {
		iter.nextOption().flatMap(wrapped.get).flatMap {
			case Right(v) => Some(v)
			// Delegates the search to the nested map, if necessary
			case Left(map) => map._apply(iter)
		}
	}
	private def _getNested(iter: Iterator[K]): Option[DeepMap[K, V]] = {
		if (iter.hasNext)
			getNested(iter.next()).flatMap { _._getNested(iter) }
		else
			Some(this)
	}
	private def _contains(iter: Iterator[K]): Boolean = {
		if (iter.hasNext) {
			val key = iter.next()
			wrapped.get(key).exists {
				case Right(_) => true
				case Left(map) => map._contains(iter)
			}
		}
		else
			false
	}
	private def _plusMany[V2 >: V](items: Iterator[(Iterator[K], V2)]): DeepMap[K, V2] = {
		// Divides additions into direct values and nested values
		val (nested, direct) = items.filter { _._1.hasNext }.divideWith { case (iter, value) =>
			val key = iter.next()
			if (iter.hasNext)
				Left((key, iter, value))
			else
				Right(key -> Right(value))
		}
		// Groups nested values by key
		val deepValues = nested.groupMap { _._1 } { case (_, iter, value) => iter -> value }
		// Merges nested values with existing maps
		val mergedDeepValues = deepValues.map { case (key, values) =>
			val newMap = getNested(key) match {
				case Some(existing) => existing._plusMany(values.iterator)
				case None => DeepMap[K, V2](values)
			}
			key -> Left(newMap)
		}
		
		copy(wrapped ++ direct ++ mergedDeepValues)
	}
	private def _plus[V2 >: V](iter: Iterator[K], value: V2): DeepMap[K, V2] = {
		if (iter.hasNext) {
			val key = iter.next()
			if (iter.hasNext)
				getNested(key) match {
					case Some(map) => copy(wrapped + (key -> Left(map._plus(iter, value))))
					case None => withMap(key, DeepMap._apply[K, V2](iter, value))
				}
			else
				withValue(key, value)
		}
		else
			this
	}
	private def _plusMap[V2 >: V](iter: Iterator[K], map: DeepMap[K, V2]): DeepMap[K, V2] = {
		if (iter.hasNext) {
			val key = iter.next()
			val newMap: DeepMap[K, V2] = {
				if (iter.hasNext)
					getNested(key) match {
						case Some(existingMap) => existingMap._plusMap(iter, map)
						case None => DeepMap.nested(iter, map)
					}
				else
					getNested(key) match {
						case Some(existingMap) => existingMap ++ map
						case None => map
					}
			}
			copy(wrapped + (key -> Left(newMap)))
		}
		else
			map
	}
	private def _minus(iter: Iterator[K]): DeepMap[K, V] = {
		if (iter.hasNext) {
			val key = iter.next()
			if (iter.hasNext)
				wrapped.get(key) match {
					case Some(existing) =>
						existing match {
							case Right(_) => this
							case Left(map) => copy(wrapped + (key -> Left(map._minus(iter))))
						}
					case None => this
				}
			else if (wrapped.contains(key))
				copy(wrapped - key)
			else
				this
		}
		else
			this
	}
}

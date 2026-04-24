package utopia.flow.collection.immutable

import utopia.flow.collection.immutable.PartialMapView.{FlatKeyMappedMapView, FlatMappedValuesView, KeyMappedMapView, MappedValuesView}
import utopia.flow.collection.template.MapAccess
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy

import scala.collection.MapView

object PartialMapView
{
	// OTHER    -------------------------
	
	/**
	 * Converts a map into a partial map view
	 * @param map A map to wrap
	 * @tparam K Type of the map's keys
	 * @tparam V Type of the map's values
	 * @return A view into that map
	 */
	def wrap[K, V](map: Map[K, V]): PartialMapView[K, V] = new MapWrapper[K, V](map)
	/**
	 * Converts a MapView into a partial map view
	 * @param mapView A map view to wrap
	 * @tparam K Type of the map's keys
	 * @tparam V Type of the map's values
	 * @return A view into that map view
	 */
	def wrap[K, V](mapView: MapView[K, V]): PartialMapView[K, V] = new MapViewWrapper[K, V](mapView)
	
	/**
	 * @param createMap A function which constructs the map to wrap, once it is needed
	 * @tparam K Type of the keys accepted
	 * @tparam V Type of the values yielded
	 * @return A map view that initializes the specified map lazily
	 */
	def wrapLazily[K, V](createMap: => Map[K, V]) = wrapView { Lazy { wrap(createMap) } }
	
	/**
	 * Wraps a map view -view
	 * @param view View that yields the map view delegate
	 * @tparam K Type of keys accepted
	 * @tparam V Type of values yielded
	 * @return A map view that wraps the specified view
	 */
	def wrapView[K, V](view: View[PartialMapView[K, V]]): PartialMapView[K, V] = new ViewWrapper[K, V](view)
	
	/**
	 * @param value Value to yield for every call
	 * @tparam V Type of the stored value
	 * @return A new map view that always yields the specified value
	 */
	def alwaysYield[V](value: V): PartialMapView[Any, V] = alwaysYieldFrom(View.fixed(value))
	/**
	 * @param valueView View to the value to yield for every call
	 * @tparam V Type of the stored value
	 * @return A new map view that always yields the specified value
	 */
	def alwaysYieldFrom[V](valueView: View[V]): PartialMapView[Any, V] = new ValueViewWrapper[V](valueView)
	
	
	// NESTED   -------------------------
	
	private class MapWrapper[-K, +V](wrapped: Map[K, V]) extends PartialMapView[K, V]
	{
		override def valuesIterator: Iterator[V] = wrapped.valuesIterator
		
		override def apply(key: K): V = wrapped(key)
		override def get(key: K): Option[V] = wrapped.get(key)
		
		override def contains(key: K): Boolean = wrapped.contains(key)
	}
	private class MapViewWrapper[-K, +V](wrapped: MapView[K, V]) extends PartialMapView[K, V]
	{
		override def valuesIterator: Iterator[V] = wrapped.valuesIterator
		
		override def apply(key: K): V = wrapped(key)
		override def get(key: K): Option[V] = wrapped.get(key)
		
		override def contains(key: K): Boolean = wrapped.contains(key)
	}
	
	private class ViewWrapper[-K, +V](view: View[PartialMapView[K, V]]) extends PartialMapView[K, V]
	{
		override def valuesIterator: Iterator[V] = view.valueIterator.flatMap { _.valuesIterator }
		
		override def apply(key: K): V = view.value(key)
		override def get(key: K): Option[V] = view.value.get(key)
		
		override def contains(key: K): Boolean = view.value.contains(key)
	}
	
	private class ValueViewWrapper[+V](valueView: View[V]) extends PartialMapView[Any, V]
	{
		override def valuesIterator: Iterator[V] = valueView.valueIterator
		
		override def apply(key: Any): V = valueView.value
		override def get(key: Any): Option[V] = Some(valueView.value)
		
		override def contains(key: Any): Boolean = true
	}
	
	private class KeyMappedMapView[-KE, -KO, +V](wrapped: PartialMapView[KO, V])(f: KE => KO)
		extends PartialMapView[KE, V]
	{
		// IMPLEMENTED  ------------------------
		
		override def valuesIterator = wrapped.valuesIterator
		
		override def apply(key: KE): V = wrapped(f(key))
		override def get(key: KE) = wrapped.get(f(key))
		
		override def contains(key: KE) = wrapped.contains(f(key))
	}
	private class FlatKeyMappedMapView[-KE, -KO, +V](wrapped: PartialMapView[KO, V])(f: KE => Option[KO])
		extends PartialMapView[KE, V]
	{
		override def valuesIterator: Iterator[V] = wrapped.valuesIterator
		
		override def apply(key: KE): V =
			wrapped(f(key).getOrElse { throw new IllegalArgumentException(s"Can't map $key") })
		override def get(key: KE): Option[V] = f(key).flatMap(wrapped.get)
		
		override def contains(key: KE): Boolean = f(key).exists(wrapped.contains)
	}
	
	private class MappedValuesView[-K, +VO, +VE](wrapped: PartialMapView[K, VO])(f: VO => VE)
		extends PartialMapView[K, VE]
	{
		override def valuesIterator: Iterator[VE] = wrapped.valuesIterator.map(f)
		
		override def apply(key: K): VE = f(wrapped(key))
		override def get(key: K): Option[VE] = wrapped.get(key).map(f)
		
		override def contains(key: K): Boolean = wrapped.contains(key)
	}
	private class FlatMappedValuesView[-K, +VO, +VE](wrapped: PartialMapView[K, VO])(f: VO => Option[VE])
		extends PartialMapView[K, VE]
	{
		override def valuesIterator: Iterator[VE] = wrapped.valuesIterator.flatMap(f)
		
		override def apply(key: K): VE = {
			val original = wrapped(key)
			f(original).getOrElse { throw new IllegalArgumentException(s"Can't map $original acquired for $key") }
		}
		override def get(key: K): Option[VE] = wrapped.get(key).flatMap(f)
		
		override def contains(key: K): Boolean = get(key).isDefined
	}
}

/**
 * Provides a view into a map, applying a mapping function to the applied keys
 * @author Mikko Hilpinen
 * @since 13.11.2025, v2.8
 */
trait PartialMapView[-K, +V] extends MapAccess[K, V]
{
	// ABSTRACT    ------------------------
	
	/**
	 * @return An iterator that yields all values accessible via this map view
	 */
	def valuesIterator: Iterator[V]
	
	/**
	 * @param key A key
	 * @return Value associated with the specified key. None if this map didn't contain the specified key.
	 */
	def get(key: K): Option[V]
	/**
	 * @param key A key
	 * @return Whether this map contains the specified key
	 */
	def contains(key: K): Boolean
	
	
	// OTHER    ---------------------------
	
	/**
	 * @param f A mapping function applied to the new keys. Yields keys compatible with this map.
	 * @tparam K2 Type of the new keys
	 * @return A map that maps the keys using 'f', before passing them to this map
	 */
	def mapInput[K2](f: K2 => K): PartialMapView[K2, V] = new KeyMappedMapView[K2, K, V](this)(f)
	/**
	 * @param f A mapping function applied to the new keys. Yields keys compatible with this map. May yield None.
	 * @tparam K2 Type of the new keys
	 * @return A map that maps the keys using 'f', before passing them to this map.
	 *         In cases where 'f' yields None, no value may be acquired.
	 */
	def flatMapInput[K2](f: K2 => Option[K]): PartialMapView[K2, V] = new FlatKeyMappedMapView[K2, K, V](this)(f)
	
	/**
	 * @param f A mapping function applied to the generated values.
	 * @tparam V2 Type of the new values
	 * @return A map that maps the values of this one using 'f'
	 */
	def mapOutput[V2](f: V => V2): PartialMapView[K, V2] = new MappedValuesView[K, V, V2](this)(f)
	/**
	 * @param f A mapping function applied to the generated values. May yield None.
	 * @tparam V2 Type of the new values
	 * @return A map that maps the values of this one using 'f'.
	 *         Values that yield None are treated as if they don't exist.
	 */
	def flatMapOutput[V2](f: V => Option[V2]): PartialMapView[K, V2] = new FlatMappedValuesView[K, V, V2](this)(f)
}

package utopia.flow.collection.template

import scala.language.implicitConversions

object MapAccess
{
	// IMPLICIT ----------------------------
	
	implicit def wrap[K, V](map: collection.Map[K, V]): MapAccess[K, V] = new MapWrapper[K, V](map)
	implicit def wrapView[K, V](mapView: collection.MapView[K, V]): MapAccess[K, V] = new MapViewWrapper[K, V](mapView)
	
	/**
	 * @param f A function that serves as a mapping
	 * @tparam K Type of mapping keys
	 * @tparam V Type of mapping values
	 * @return A map that is solely based on the specified function
	 */
	implicit def apply[K, V](f: K => V): MapAccess[K, V] = new MapLikeFunction[K, V](f)
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param map A function that yields the map to wrap. Called the first time a value is requested.
	 * @tparam K Type of used keys
	 * @tparam V Type of yielded values
	 * @return A map access that lazily wraps the specified map
	 */
	def wrapLazily[K, V](map: => collection.Map[K, V]): MapAccess[K, V] = new LazyMapWrapper[K, V](map)
	
	
	// NESTED   ----------------------------
	
	/**
	  * A function that acts as a map-like instance
	  * @param f Wrapped function
	  * @tparam K Type of keys used
	  * @tparam V Types of values returned
	  */
	private class MapLikeFunction[-K, +V](f: K => V) extends MapAccess[K, V]
	{
		override def apply(key: K) = f(key)
	}
	
	private class MapWrapper[-K, +V](map: collection.Map[K, V]) extends MapAccess[K, V]
	{
		override def apply(key: K): V = map(key)
	}
	private class MapViewWrapper[-K, +V](mapView: collection.MapView[K, V]) extends MapAccess[K, V]
	{
		override def apply(key: K): V = mapView(key)
	}
	private class LazyMapWrapper[K, +V](getMap: => collection.Map[K, V]) extends MapAccess[K, V]
	{
		private lazy val map = getMap
		
		override def apply(key: K): V = map(key)
	}
}

/**
  * A common trait for datastructures that offer a key-value access similar to that of Maps,
  * although an additional expectation with this trait is that all keys yield a value of some sort.
  * @author Mikko Hilpinen
  * @since 20.7.2021, v1.11
  */
trait MapAccess[-K, +V]
{
	/**
	  * Accesses an individual value in this structure
	  * @param key Key to access
	  * @return Value for that key
	  */
	def apply(key: K): V
}

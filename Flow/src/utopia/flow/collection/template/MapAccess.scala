package utopia.flow.collection.template

import scala.language.implicitConversions

object MapAccess
{
	// IMPLICIT ----------------------------
	
	// Implicitly converts functions to map-like items
	implicit def functionToMapLike[K, V](f: K => V): MapAccess[K, V] = new MapLikeFunction[K, V](f)
	
	
	// OTHER    -----------------------------
	
	/**
	 * @param f A function that serves as a mapping
	 * @tparam K Type of mapping keys
	 * @tparam V Type of mapping values
	 * @return A map that is solely based on the specified function
	 */
	def apply[K, V](f: K => V): MapAccess[K, V] = new MapLikeFunction[K, V](f)
	
	
	// NESTED   ----------------------------
	
	/**
	  * A function that acts as an map-like instance
	  * @param f Wrapped function
	  * @tparam K Type of keys used
	  * @tparam V Types of values returned
	  */
	private class MapLikeFunction[-K, +V](f: K => V) extends MapAccess[K, V]
	{
		override def apply(key: K) = f(key)
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

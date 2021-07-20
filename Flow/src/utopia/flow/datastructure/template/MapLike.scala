package utopia.flow.datastructure.template

import scala.language.implicitConversions

object MapLike
{
	// IMPLICIT ----------------------------
	
	// Implicitly converts functions to map-like items
	implicit def functionToMapLike[K, V](f: K => V): MapLike[K, V] = new MapLikeFunction[K, V](f)
	
	
	// NESTED   ----------------------------
	
	/**
	  * A function that acts as an map-like instance
	  * @param f Wrapped function
	  * @tparam K Type of keys used
	  * @tparam V Types of values returned
	  */
	class MapLikeFunction[-K, +V](f: K => V) extends MapLike[K, V]
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
trait MapLike[-K, +V]
{
	/**
	  * Accesses an individual value in this structure
	  * @param key Key to access
	  * @return Value for that key
	  */
	def apply(key: K): V
}

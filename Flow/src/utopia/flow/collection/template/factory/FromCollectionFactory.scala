package utopia.flow.collection.template.factory

import utopia.flow.collection.immutable.Pair

/**
  * Common trait for factory classes / objects that wrap / accept a collection of items
  * @author Mikko Hilpinen
  * @since 07/02/2024, v2.4
  * @tparam A Type of collected items
  * @tparam C Type of constructed collections
  */
trait FromCollectionFactory[-A, +C]
{
	// ABSTRACT ----------------------
	
	/**
	  * @param items A collection of items
	  * @return A collection that wraps those items
	  */
	def from(items: IterableOnce[A]): C
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return An empty collection
	  */
	def empty = from(Iterable.empty)
	
	
	// OTHER    ----------------------
	
	/**
	  * Alias for [[empty]]
	  * @return An empty collection
	  */
	def apply() = empty
	/**
	  * @param item An item to place in this collection
	  * @return A collection wrapping that single item
	  */
	def apply(item: A): C = from(Iterable.single(item))
	/**
	  * @return A collection wrapping the specified items
	  */
	def apply(item: A, another: A, more: A*): C = from(Pair(item, another) ++ more)
}

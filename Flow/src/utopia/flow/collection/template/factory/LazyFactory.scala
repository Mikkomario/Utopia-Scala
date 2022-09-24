package utopia.flow.collection.template.factory

import utopia.flow.collection.mutable.iterator.PollableOnce
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.template.LazyLike

/**
  * A common trait for factories that produce lazy collections
  * @author Mikko Hilpinen
  * @since 24.7.2022, v1.16
  */
trait LazyFactory[+Coll[X]]
{
	// ABSTRACT ----------------------------
	
	/**
	  * Creates a new empty collection
	  * @tparam A Type of collection items
	  * @return A new empty collection
	  */
	def apply[A](): Coll[A]
	
	/**
	  * Creates a new collection with the specified lazily initialized items
	  * @param items Items to wrap within this collection
	  * @tparam A Type of lazily initialized items
	  * @return A new collection with those items
	  */
	def apply[A](items: IterableOnce[LazyLike[A]]): Coll[A]
	
	
	// OTHER    ----------------------------
	
	def apply[A](first: LazyLike[A], more: LazyLike[A]*): Coll[A] = apply(first +: more)
	
	/**
	  * Creates a new collection containing only a single item
	  * @param item Item to wrap (call-by-name / lazily initialized)
	  * @tparam A Type of that item
	  * @return A collection that will contain that item
	  */
	def single[A](item: => A) = apply(PollableOnce(Lazy(item)))
	
	def fromFunctions[A](functions: IterableOnce[() => A]) =
		apply(functions.iterator.map { f => Lazy(f()) })
	
	def fromFunctions[A](functions: (() => A)*) = apply(functions.iterator.map { f => Lazy(f()) })
}

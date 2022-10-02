package utopia.flow.collection.mutable.iterator

import utopia.flow.view.immutable.caching.Lazy

object LazyInitIterator
{
	/**
	  * @param source A function that yields a collection to iterate
	  * @tparam A Type of collection item
	  * @return A lazily initialized iterator of that collection
	  */
	def apply[A](source: => IterableOnce[A]) = new LazyInitIterator[A](Lazy { source.iterator })
}

/**
  * An iterator that initializes itself lazily
  * @author Mikko Hilpinen
  * @since 28.9.2022, v2.0
  */
class LazyInitIterator[+A](source: Lazy[Iterator[A]]) extends Iterator[A]
{
	override def hasNext = source.value.hasNext
	override def next() = source.value.next()
}

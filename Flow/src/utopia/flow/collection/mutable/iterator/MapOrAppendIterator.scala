package utopia.flow.collection.mutable.iterator

import utopia.flow.view.mutable.Settable

object MapOrAppendIterator
{
	/**
	 * @param source Source collection to map / modify
	 * @param f A mapping function to perform.
	 *          Yields None for cases where the original item should be preserved.
	 *          Called for each item in 'source' until Some is yielded.
	 * @param append A function that generates a new item.
	 *               Only called (once) if 'f' yielded None of all items in 'source'.
	 * @tparam A Type of the source collection items
	 * @tparam B Type of the mapped items
	 * @return A new iterator that transforms one item, or appends one item
	 */
	def apply[A, B >: A](source: IterableOnce[A])(f: A => Option[B])(append: => B) =
		new MapOrAppendIterator[A, B](source.iterator)(f)(append)
}

/**
 * An iterator that maps an item or appends an item to an existing iterator.
 * @author Mikko Hilpinen
 * @since 13.09.2026, v2.9
 */
class MapOrAppendIterator[-A, +B >: A](source: Iterator[A])(f: A => Option[B])(append: => B) extends Iterator[B]
{
	// ATTRIBUTES   --------------------------
	
	private val mappedFlag = Settable()
	
	
	// IMPLEMENTED  --------------------------
	
	override def hasNext: Boolean = mappedFlag.isNotSet || source.hasNext
	
	override def next(): B = source.nextOption() match {
		case Some(a) =>
			// Case: Mapping was already performed => Yields the remaining source item(s) as-is
			if (mappedFlag.isSet)
				a
			// Case: No mapping performed yet => Attempts to map the next item
			else
				f(a) match {
					// Case: Mapping performed => Remembers & yields the mapped item
					case Some(mapped) =>
						mappedFlag.set()
						mapped
					// Case: Not the mapping target => Yields the original item
					case None => a
				}
		// Case: No more source items => Appends a new item unless mapped or appended already
		case None =>
			if (mappedFlag.set())
				append
			// Case: Already mapped or appended => Throws
			else
				throw new NoSuchElementException("next() called on an empty iterator")
	}
}

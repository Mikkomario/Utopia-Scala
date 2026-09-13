package utopia.flow.collection.mutable.iterator

import utopia.flow.view.mutable.Settable
import utopia.flow.view.template.MaybeSet

object MapFirstWhereIterator
{
	/**
	 * @param source A source collection to map
	 * @param find A function that yields true for the item to map
	 * @param map A mapping function to apply (called 0-1 times)
	 * @tparam A Type of items in the original collection
	 * @tparam B Type of the mapped item
	 * @return An iterator that maps the first entry for which 'find' yields true.
	 */
	def apply[A, B >: A](source: IterableOnce[A])(find: A => Boolean)(map: A => B) =
		new MapFirstWhereIterator[A, B](source.iterator)(find)(map)
}

/**
 * An Iterator implementation that maps up to one item from another collection
 * @tparam A Type of the items in the source collection
 * @tparam B Type of the mapped item
 * @author Mikko Hilpinen
 * @since 13.09.2026, v2.9
 */
class MapFirstWhereIterator[-A, +B >: A](source: Iterator[A])(find: A => Boolean)(map: A => B) extends Iterator[B]
{
	// ATTRIBUTES   -------------------------
	
	private val _mappedFlag = Settable()
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return A flag that contains true after an item has been found / mapped
	 */
	def mappedFlag = MaybeSet.view(_mappedFlag)
	
	
	// IMPLEMENTED  -------------------------
	
	override def hasNext: Boolean = source.hasNext
	
	override def next(): B = {
		val a = source.next()
		if (_mappedFlag.isNotSet && find(a)) {
			_mappedFlag.set()
			map(a)
		}
		else
			a
	}
}

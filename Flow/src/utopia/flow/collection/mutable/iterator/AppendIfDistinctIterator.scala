package utopia.flow.collection.mutable.iterator

import utopia.flow.view.mutable.caching.ResettableLazy

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable

object AppendIfDistinctIterator
{
	/**
	 * @param coll Collection being appended
	 * @param addition Items to append
	 * @tparam A Type of iterated items
	 * @return A new iterator that yields all items in 'coll', plus items in 'addition' that were not present in 'coll'.
	 */
	def apply[A](coll: IterableOnce[A], addition: IterableOnce[A]) =
		new AppendIfDistinctIterator(coll.iterator, addition.iterator)
}

/**
 * An iterator that appends distinct items to a source iterator.
 * Preservers duplicates within the source itself.
 * @author Mikko Hilpinen
 * @since 25.09.2026, v2.9
 */
class AppendIfDistinctIterator[+A](source: Iterator[A], addition: Iterator[A]) extends Iterator[A]
{
	// ATTRIBUTES   -----------------------
	
	private val sourceValuesBuffer: mutable.Set[A @uncheckedVariance] = mutable.Set[A]()
	private val polledDistinct = ResettableLazy { addition.find { !sourceValuesBuffer.contains(_) } }
	
	
	// IMPLEMENTED  -----------------------
	
	override def hasNext: Boolean = source.hasNext || polledDistinct.value.isDefined
	
	override def next(): A = source.nextOption() match {
		case Some(a) =>
			sourceValuesBuffer += a
			a
		case None => polledDistinct.pop().get
	}
}
